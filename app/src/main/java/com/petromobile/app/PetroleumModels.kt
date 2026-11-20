package com.petromobile.app

import kotlin.math.pow

/**
 * Independent black-oil / well-performance correlations.
 * Field units: pressure psi, temperature °F, Rs scf/STB, Bo rb/STB.
 * Correlations are empirical and must be checked against PVT laboratory data.
 */
data class PvtSample(
    val pressurePsi: Double,
    val solutionGasOilRatioScfStb: Double,
    val oilFormationVolumeFactorRbStb: Double
)

object PetroleumModels {
    private fun requirePositive(name: String, value: Double) {
        require(value.isFinite() && value > 0.0) { "$name must be a finite positive number." }
    }

    /** Standing bubble-point pressure correlation. */
    fun standingBubblePointPsi(
        api: Double,
        gasSpecificGravity: Double,
        temperatureF: Double,
        solutionGasOilRatio: Double
    ): Double {
        requirePositive("API gravity", api)
        requirePositive("Gas specific gravity", gasSpecificGravity)
        require(temperatureF.isFinite() && temperatureF > -459.67) { "Temperature must be above absolute zero." }
        require(solutionGasOilRatio.isFinite() && solutionGasOilRatio >= 0.0) { "Rs cannot be negative." }
        val inner = (solutionGasOilRatio / gasSpecificGravity).pow(0.83) *
            10.0.pow(0.00091 * temperatureF - 0.0125 * api)
        return 18.2 * (inner - 1.4).coerceAtLeast(0.0).pow(1.0 / 0.83)
    }

    /** Standing solution GOR at pressure, capped at the supplied bubble point. */
    fun standingSolutionGasOilRatio(
        pressurePsi: Double,
        bubblePointPsi: Double,
        api: Double,
        gasSpecificGravity: Double,
        temperatureF: Double
    ): Double {
        require(pressurePsi.isFinite() && pressurePsi >= 0.0) { "Pressure cannot be negative." }
        requirePositive("Bubble-point pressure", bubblePointPsi)
        requirePositive("API gravity", api)
        requirePositive("Gas specific gravity", gasSpecificGravity)
        require(temperatureF.isFinite() && temperatureF > -459.67) { "Temperature must be above absolute zero." }
        val p = pressurePsi.coerceAtMost(bubblePointPsi)
        val term = (p / 18.2 + 1.4) *
            10.0.pow(0.0125 * api - 0.00091 * temperatureF)
        return gasSpecificGravity * term.pow(1.2048)
    }

    /** Standing oil FVF correlation for saturated oil, field units. */
    fun standingOilFvf(
        solutionGasOilRatio: Double,
        gasSpecificGravity: Double,
        api: Double,
        temperatureF: Double
    ): Double {
        require(solutionGasOilRatio.isFinite() && solutionGasOilRatio >= 0.0) { "Rs cannot be negative." }
        requirePositive("Gas specific gravity", gasSpecificGravity)
        requirePositive("API gravity", api)
        require(temperatureF.isFinite() && temperatureF > -459.67) { "Temperature must be above absolute zero." }
        val oilSpecificGravity = 141.5 / (api + 131.5)
        val term = solutionGasOilRatio * (gasSpecificGravity / oilSpecificGravity).pow(0.5) +
            1.25 * temperatureF
        return 0.972 + 0.000147 * term.coerceAtLeast(0.0).pow(1.175)
    }

    /** Vogel inflow rate from AOF, reservoir pressure and flowing BHP. */
    fun vogelRate(aofStbDay: Double, reservoirPressurePsi: Double, flowingPressurePsi: Double): Double {
        requirePositive("AOF", aofStbDay)
        requirePositive("Reservoir pressure", reservoirPressurePsi)
        require(flowingPressurePsi.isFinite() && flowingPressurePsi >= 0.0 &&
            flowingPressurePsi <= reservoirPressurePsi) { "Flowing pressure must be between zero and reservoir pressure." }
        val x = flowingPressurePsi / reservoirPressurePsi
        return aofStbDay * (1.0 - 0.2 * x - 0.8 * x * x)
    }

    /**
     * Composite IPR using a supplied productivity index J:
     * linear above Pb and Vogel below Pb, continuous at Pb.
     * Returns oil rate in STB/day.
     */
    fun compositeIpRRate(
        productivityIndexStbDayPsi: Double,
        reservoirPressurePsi: Double,
        bubblePointPsi: Double,
        flowingPressurePsi: Double
    ): Double {
        requirePositive("Productivity index", productivityIndexStbDayPsi)
        requirePositive("Reservoir pressure", reservoirPressurePsi)
        requirePositive("Bubble-point pressure", bubblePointPsi)
        require(bubblePointPsi <= reservoirPressurePsi) { "Bubble-point pressure cannot exceed reservoir pressure." }
        require(flowingPressurePsi.isFinite() && flowingPressurePsi >= 0.0 &&
            flowingPressurePsi <= reservoirPressurePsi) { "Flowing pressure must be between zero and reservoir pressure." }
        return if (flowingPressurePsi >= bubblePointPsi) {
            productivityIndexStbDayPsi * (reservoirPressurePsi - flowingPressurePsi)
        } else {
            val rateAtBubblePoint = productivityIndexStbDayPsi * (reservoirPressurePsi - bubblePointPsi)
            rateAtBubblePoint + productivityIndexStbDayPsi * bubblePointPsi / 1.8 *
                (1.0 - 0.2 * flowingPressurePsi / bubblePointPsi -
                    0.8 * (flowingPressurePsi / bubblePointPsi).pow(2))
        }
    }

    fun standingPvtTable(
        bubblePointPsi: Double,
        api: Double,
        gasSpecificGravity: Double,
        temperatureF: Double,
        points: Int = 41
    ): List<PvtSample> {
        require(points >= 2) { "At least two pressure points are required." }
        requirePositive("Bubble-point pressure", bubblePointPsi)
        return (0 until points).map { index ->
            val pressure = bubblePointPsi * index / (points - 1).toDouble()
            val rs = standingSolutionGasOilRatio(
                pressure, bubblePointPsi, api, gasSpecificGravity, temperatureF
            )
            PvtSample(
                pressurePsi = pressure,
                solutionGasOilRatioScfStb = rs,
                oilFormationVolumeFactorRbStb = standingOilFvf(rs, gasSpecificGravity, api, temperatureF)
            )
        }
    }
}
