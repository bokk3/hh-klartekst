# Heidenhain Klartext Simulator (Android App)

Een geavanceerde Android applicatie gebouwd met **Kotlin** en **Jetpack Compose** om CNC **Heidenhain Klartext** (`.H`) programma's te simuleren, visualiseren en controleren.

---

## 🚀 Kenmerken

- **Volledige Klartext Parser & Interpreter**:
  - Ondersteuning voor `BEGIN PGM` / `END PGM` (MM en INCH)
  - Ruwdeeldefinitie (`BLK FORM 0.1` en `0.2`)
  - Gereedschapswissels (`TOOL CALL`, `TOOL DEF` met radius $R$ en lengte $L$)
  - Lineaire verplaatsingen (`L`) met ijlgang (`FMAX`), voeding (`F`), radiuscompensatie (`RL`/`RR`/`R0`) en absolute/incidentele coördinaten (`X`, `IX`, `Y`, `IY`, `Z`, `IZ`)
  - Cirkelbanen (`CC`, `C`, `CR`) inclusief draairichting (`DR+`/`DR-`)
  - Subroutines & herhalingen (`LBL SET`, `CALL LBL REP`)
  - Vaste cycli (`CYCL DEF`, `CYCL CALL`)
- **Interactieve Visualizer (Canvas)**:
  - Kleurgecodeerde freesbanen:
    - 🔴 **Rood gestippeld**: IJlgang (`FMAX`)
    - 🔵 **Cyaan**: Freesvoeding (`F`)
    - 🟣 **Paars**: Insteekbewegingen in de Z-as
  - Ruwdeelweergave (`BLK FORM`) als transparant blok
  - Gereedschapspositie en freesdiameter weergave
  - Pan & zoom ondersteuning
  - Meerdere aanzichten: **Boven (XY)**, **Voor (XZ)**, **Zij (YZ)** en **Isometrisch (3D)**
- **Digitale Uitlezing (DRO)**:
  - CNC-stijl weergave van actuele coördinaten ($X$, $Y$, $Z$) tot op $0.001\text{ mm}$
  - Realtime status van voeding, spiltoerental, actieve gereedschap en compensatiemodus
- **Geïntegreerde Code Viewer**:
  - Syntax-highlighting van Klartext commando's
  - Automatisch meescrollen en markeren van het actieve blok tijdens simulatie
  - Klikken op een regel springt direct naar dat punt in de freesbaan
- **Afspeelbediening**:
  - Play, Pauze, Stap vooruit, Stap terug, Reset
  - Tijdlijnschuifregelaar (scrubbing)
  - Variabele afspeelsnelheid ($0.5\times$ tot $5\times$)
  - Voorgeprogrammeerde voorbeelden (Buitencontour met afrondingen, Ronde kamer, Gatenpatroon)

---

## 🛠️ Schijfruimte & Installatie op D:

Alle zware Android-bestanden zijn geconfigureerd op de **D:-schijf** om de C:-schijf te ontlasten:
- **Android Studio IDE**: `D:\Android\AndroidStudio`
- **Android SDK**: `D:\Android\Sdk` (omgevingsvariabele `ANDROID_HOME` en `ANDROID_SDK_ROOT`)
- **Gradle Caches**: `D:\Android\.gradle` (omgevingsvariabele `GRADLE_USER_HOME`)
- **AVD Emulators**: `D:\Android\.android\avd` (omgevingsvariabele `ANDROID_AVD_HOME`)

---

## 📱 Openen in Android Studio

1. Start **Android Studio**.
2. Kies **Open** en selecteer de map `C:\Users\Boris\android-app`.
3. Android Studio synchroniseert het Gradle-project automatisch en installeert de benodigde SDK componenten naar `D:\Android\Sdk`.
4. Druk op **Run** (groene afspeelknop) of verbind een Android-toestel via USB debugging / start een emulator.
