# PetroMobile
Independent Android learning prototype for petroleum well engineering.

## Prototype 0.1
- Kotlin + Jetpack Compose
- Well name field
- Simplified Vogel IPR calculator and graph
- Minimum Android API 26

## Build APK
GitHub Actions runs on pushes to main. Open Actions, select the latest Build PetroMobile APK run, and download the PetroMobile-debug-apk artifact. Extract the artifact ZIP and install app-debug.apk.

## Engineering limitation
The current model uses q = qmax [1 - 0.2(Pwf/Pr) - 0.8(Pwf/Pr)^2]. Bubble-point pressure is validated but not used in a composite IPR model. This is an educational prototype, not a PROSPER-equivalent application and not suitable for operational decisions.

Build uses Java 17 for both Java and Kotlin compilation.
