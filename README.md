# HEIDENHAIN Klartext CNC Simulator & Opleidingsplatform (Android)

[![Platform: Android](https://img.shields.io/badge/Platform-Android-3DDC84.svg?style=flat&logo=android)](https://developer.android.com)
[![Kotlin](https://img.shields.io/badge/Kotlin-1.9+-7F52FF.svg?style=flat&logo=kotlin)](https://kotlinlang.org)
[![Jetpack Compose](https://img.shields.io/badge/Jetpack%20Compose-Declarative%20UI-4285F4.svg?style=flat&logo=jetpackcompose)](https://developer.android.com/jetpack/compose)
[![Controller: HEIDENHAIN TNC 640](https://img.shields.io/badge/Controller-HEIDENHAIN%20TNC%20640-FF9100.svg?style=flat)](https://www.heidenhain.nl)
[![Tests](https://img.shields.io/badge/Unit%20Tests-100%25%20Passing-brightgreen.svg?style=flat)](app/src/test/java/com/klartext/simulator/)

Een industriële en didactische Android-applicatie voor het simuleren, verifiëren, visualiseren en aanleren van **HEIDENHAIN Klartext (`.H`)** CNC-freesprogramma's.

De app combineert een getrouwe virtuele **HEIDENHAIN TE 640 bedieningsconsole**, een real-time **3D Orbit software render-engine**, een **2.5D volumetrische materiaalafname-engine** en een compleet **opleidingscurriculum** gebaseerd op het lesmateriaal van [github.com/bokk3/heidenhain](https://github.com/bokk3/heidenhain).

---

## 📑 Uitgebreide Documentatie
Voor gedetailleerde technische specificaties en lesmateriaal verwijzen we naar de documentatiemap:
- 📖 [**HEIDENHAIN Opleidingscurriculum & Praktijkoefeningen**](docs/HEIDENHAIN_CURRICULUM.md): Volledige uitwerking van de 5 officiële tekeningen (1226651, 1206129, 1214098, 1214155, 1226642), inclusief wiskundige formules, didactische doelen en examenvragen.
- 📐 [**Klartext Taalreferentie & Syntaxhandleiding**](docs/KLARTEXT_LANGUAGE_REFERENCE.md): Volledige specificatie van alle Klartext-blokken (`L`, `LP`, `CC`, `C`, `CR`, `CT`, `CP`, `RND`, `CHF`, `APPR`, `DEP`, `LBL`), vaste cycli (200, 206, 207, 208, 220, 221, 232, 240, 251, 252, 253, SL 14/20/22/23/24), Q-parameter wiskunde (`FN 0..12`) en M-codes.
- ⚙️ [**Technische Architectuur & Systeemdocumentatie**](docs/ARCHITECTURE.md): Systeemontwerp, MVI dataflow, 3D Euler-projectiematrices, touchscreen hoekvergrendelingsalgoritmen, 2.5D heightmap voxel-stamping en CAD/ISO importpipelines.

---

## 🚀 Kernfunctionaliteiten

### 1. 3D Orbit Software Render-Engine & Assen-Oriëntatie
- **Volledige 3D Vrijheid**: Inspecteer werkstukken vanuit elke hoek met dynamische Yaw (Z-as), Pitch (X-as) en Roll (Y-as) rotaties.
- **Correcte Industriële Z-As**: $+Z$ wijst loodrecht naar boven. Het gereedschap steekt van boven in het werkstuk, en de machineklem bevindt zich aan de onderzijde.
- **Machineklem (Vise)**: Automatische dimensionering en weergave van stalen machineklem en parallelonderleggers onder het ruwdeel ($Z \le Z_{min}$).
- **3D Assenkruis (Gizmo)**: Kleurgecodeerde vectoren conform CAD/CAM-normen: 🔴 Rood ($+X$), 🟢 Groen ($+Y$), 🔵 Blauw ($+Z$).
- **Gereedschapsweergave**: 3D wireframe freeslichaam met gereedschapshouder en actuele snijradius.

### 2. Touchscreen Besturing met As-Vergrendeling
Om ongewenst zwabberen over meerdere assen tijdens veegbewegingen op touchscreens te voorkomen:
- **Eén vinger (Directional Angle Locking)**:
  - Vlakke veeg ($\le 28^\circ$) $\to$ **Vergrendeld op Z-as (Yaw / C-as)**.
  - Steile veeg ($\ge 62^\circ$) $\to$ **Vergrendeld op X-as (Pitch / A-as)**.
  - Diagonale veeg ($28^\circ - 62^\circ$) $\to$ **Vergrendeld op Y-as (Roll / B-as)**.
  - Zodra de bewegingsrichting wordt gedetecteerd ($> 10\text{ px}$), roteert het model uitsluitend om die ene as!
- **Twee vingers**:
  - **Pinch-to-zoom**: Traploos zoomen van $0.4\times$ tot $25\times$.
  - **Twee vingers slepen**: Pan-verschuiving in $X$ en $Y$.
  - **Twee vingers draaien**: Vrije rotatie om de schermas.

### 3. 2.5D Volumetrische Verspaningssimulatie
- **Realtime Materiaalafname**: Hoogtemap-raster van $\approx 100 \times 100$ cellen over het ruwdeelblok.
- **Ondersteuning voor Diverse Freesprofielen**:
  - Vlakke vingerfrezen en vlakfrezen (cilindrisch).
  - $45^\circ$ vellingfrezen en $90^\circ$ centerboren (conisch profiel: $Z(r) = Z_{tip} + r$).
  - Bolfrezen (sferisch profiel: $Z(r) = Z_{tip} + R - \sqrt{R^2 - r^2}$).
  - Torusfrezen / Bull-nose met hoekstraal $R_c$.
- **Stapsgewijze Visualisatie**: Materiaalafname volgt synchroon de geselecteerde programmastap of de automatische afspeelcyclus.

### 4. TNC 640 Bedieningspaneel & Twee Visuele Thema's
- **Thema 1: CLASSIC_TNC640 (Industrieel HEIDENHAIN TE 640)**:
  - Lichtgrijs gepoedercoat chassis (RAL 7035: `#C7CED6`).
  - Ivoorwitte numerieke toetsen (`#F8FAFC`) met diepzwarte opdruk.
  - Donkergrijze grafiettoetsen (`#1E2832`) met iconische oranje contoursymbolen (`#FF9100`).
  - Blauwgrijze functietoetsen (`#243447`) voor `APPR`, `DEP`, `CYCL DEF` en `TOOL CALL`.
  - Machinegroene `ENT` en `NC START` knoppen (`#15803D`).
  - Signaalrode `NO ENT` en `NC STOP` knoppen (`#B91C1C`).
  - Geborsteld aluminium draaiknoppen voor Feed- en Spindle-override ($0\% - 150\%$).
- **Thema 2: HYPERMODERN**:
  - Donkere carbon- en leisteentinten met cyaan- en neon-oranje verlichting voor nachtelijke programmeersessies.
- **Android 17 (2027) Adaptive UI & Ergonomische Mobiele Workspaces**:
  - **Custom CNC Design System (`CncKeyCap`, `CncSoftkey`, `CncTimelineScrubber`)**: Bypast de Material 3 `48.dp` minimum touch-target border bug zodat industriële toetsrijen, numpads en softkeys nooit overlappen.
  - **TwinScreen Console View (Tablets & Grote Schermen)**: Gesplitste weergave met links het TNC-beeldscherm (DRO + 3D Sim + Code + Softkeys) en rechts het volledige HEIDENHAIN TE 640 bedieningspaneel.
  - **Doelgerichte Mobiele Workspace-Tabs (Smartphones)**:
    1. **Tab 1 — `3D SIM & UITVOER`**: Volledig scherm voor 3D/2D simulatie met zwevende mini-DRO, scrollbare weergave-chips, live coderegel-ticker en een dedicated Machine Uitvoer- en Override-paneel (`NC START`, `NC STOP`, `SINGLE`, `STAP`, Feed/Spindle Overrides).
    2. **Tab 2 — `PROGRAMMEREN (CODE + KNOPPEN)`**: In landschap **Code links en het TNC 640 toetsenbord rechts**; in portret **Code boven en een in-/uitklapbaar CNC-toetsenbord onder**.
    3. **Tab 3 — `TOOLS & PGM`**: Ruim werkplaats-dashboard voor Gereedschapstabel, Calculator, Q-Parameters, DXF/ISO Converter, Diagnostiek en alle Opleidingsprogramma's.

### 5. Geïntegreerd Opleidingscurriculum
Direct selecteerbaar via de programmakiezer:
1. **Oefening 1 (Tekening 1226651)**: Regelmatige vijfhoek via poolcoördinaten (`CC`, `LP`, hoeken $108^\circ / 72^\circ / 36^\circ$).
2. **Oefening 2 (Tekening 1206129)**: Ruitvormig opstaand eiland via meelopend frezen (`RL`), incrementele diepteherhalingen (`CALL LBL REP`) en tangentiële nadering (`APPR LT`).
3. **Oefening 3 (Tekening 1214098)**: Boogcontour met overgangsbogen (`CR`), middelpuntberekende bogen (`C`) en ronde kameruitruiming.
4. **Oefening 4 (Tekening 1214155)**: Vaste bewerkingscycli voor rechthoekige kamers (`CYCL 251`), ronde kamers (`CYCL 252`) en sleuven (`CYCL 253`).
5. **Oefening 5 (Tekening 1226642)**: Complete 22-gaten bewerking met centerboren (`CYCL 240`), spiraalboren $\varnothing 6.8\text{ mm}$ (`CYCL 200`), draadtappen M8 (`CYCL 206`), steekcirkelpatroon (`CYCL 220`) en een subprogramma voor de lineaire gatenrij (`LBL 1`).

### 6. CAD / ISO & Werkplaats Utilities
- **AutoCAD DXF Importer (`DxfParser`)**: Importeert 2D DXF-tekeningen, detecteert automatisch aaneengesloten contouren en genereert direct schone Klartext-banen.
- **Bi-directionele ISO G-Code Converter (`IsoGCodeConverter`)**: Vertaalt DIN 66025 / ISO 6983 G-code programma's naar HEIDENHAIN Klartext en vice versa.
- **TNC 640 Verspaningscalculator**:
  - Berekening van spiltoerental $S = \frac{V_c \times 1000}{\pi \times D}$ en voeding $F = S \times z \times f_z$ voor diverse materialen (Aluminium, Constructiestaal, 42CrMo4, RVS).
  - Tapcalculator voor M3 t/m M16 met spoed, synchrone voeding $F = S \times P$ en voorboordiameter $D_{boor} = D_{nom} - P$.
  - Centerboorcalculator voor $90^\circ$ verzinkingen.
- **Gereedschapstabelbeheer**: Volledige beheerder voor gereedschappen T1 t/m T999 met radius $R$, lengte $L$, hoekradius $R_2$, tophoek $\alpha$ en slijtagecorrecties ($DL, DR$).
- **Q-Parameter Monitor & Diagnostiek**: Live inzicht in $Q0 \dots Q199$, botsingsdetectie voor gereedschapshouders en programma-tijdberekening.

---

## 🛠️ Schijfruimte & Omgevingsconfiguratie (D:-Schijf)

Om de `C:`-schijf vrij te houden, is de volledige ontwikkelomgeving geconfigureerd op de **`D:`-schijf**:

| Component | Pad op D: | Omgevingsvariabele |
| :--- | :--- | :--- |
| **Android Studio IDE** | `D:\Android\AndroidStudio` | — |
| **Android SDK** | `D:\Android\Sdk` | `ANDROID_HOME` & `ANDROID_SDK_ROOT` |
| **Gradle Caches** | `D:\Android\.gradle` | `GRADLE_USER_HOME` |
| **AVD Emulators** | `D:\Android\.android\avd` | `ANDROID_AVD_HOME` |

---

## 💻 Bouwen, Testen & Uitvoeren

### 1. Testsuite Uitvoeren
Voer alle unittests (parser, AST, cycli, radiuscompensatie, opleidingsprogramma's en thema's) uit:
```powershell
.\gradlew.bat testDebugUnitTest
```

### 2. Debug APK Bouwen
```powershell
.\gradlew.bat assembleDebug
```
Het gegenereerde installatiebestand bevindt zich in:
`app\build\outputs\apk\debug\app-debug.apk`

### 3. Installeren op AVD Emulator of Fysiek Toestel
```powershell
adb install -r app\build\outputs\apk\debug\app-debug.apk
```

---

## 👥 Bijdragen & Licentie
Ontwikkeld voor CNC-docenten, verspaners, programmeurs en studenten die HEIDENHAIN TNC-besturingen willen leren beheersen via een intuïtieve mobiele simulator.

Vragen, feedback of nieuwe cyclus-verzoeken zijn welkom via [GitHub Issues](https://github.com/bokk3/heidenhain/issues).
