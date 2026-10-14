<div align="center">

# 📚 Untis Homework

### Homework, without the daily scramble.

**One calm place to see what's due, what's done, and who it belongs to.**

An offline family homework dashboard, built with fictional schoolwork so you can explore the idea without connecting a real account.

[**See the dashboard ↓**](#see-the-dashboard) · [**Try it locally →**](#open-the-browser-demo) · [**Build and validate →**](#build-and-validate)

<br>

![Status](https://img.shields.io/badge/status-experimental%20source%20demo-536171?style=flat-square)
![Mode](https://img.shields.io/badge/browser-offline-316d74?style=flat-square)
![Android](https://img.shields.io/badge/Android-Kotlin%20%2B%20Compose-3d795c?style=flat-square)
![License](https://img.shields.io/badge/license-MIT-24836b?style=flat-square)

</div>

> **Important:** This is a fictional, offline demonstration. There is **no live Untis or WebUntis connection**, no school login, and no hosted backend. This is an independent project, not affiliated with or endorsed by Untis or WebUntis.

---

## See the dashboard

<table>
  <tr>
    <td align="center" width="50%">
      <a href="docs/screenshots/homework.png"><img src="docs/screenshots/homework.png" alt="Actual offline dashboard showing fictional homework due for Alex" width="440"></a>
    </td>
    <td align="center" width="50%">
      <a href="docs/screenshots/completed.png"><img src="docs/screenshots/completed.png" alt="Actual offline dashboard showing completed fictional homework in the archive" width="440"></a>
    </td>
  </tr>
  <tr>
    <td align="center"><strong>01 · See what's next</strong><br><sub>Homework, subject colors, and upcoming tasks.</sub></td>
    <td align="center"><strong>02 · Close the loop</strong><br><sub>Move finished work into the completed view.</sub></td>
  </tr>
  <tr>
    <td align="center">
      <a href="docs/screenshots/notes.png"><img src="docs/screenshots/notes.png" alt="Local reminder displayed in the real offline browser dashboard" width="440"></a>
    </td>
    <td align="center">
      <a href="docs/screenshots/family.png"><img src="docs/screenshots/family.png" alt="Separate fictional homework view for Robin" width="440"></a>
    </td>
  </tr>
  <tr>
    <td align="center"><strong>03 · Remember the little things</strong><br><sub>Add a note without another app.</sub></td>
    <td align="center"><strong>04 · Keep children separate</strong><br><sub>Switch between individual family views.</sub></td>
  </tr>
</table>

<sub>Genuine Chromium captures of the included browser dashboard. Names and assignments are fictional; the demonstration clock is fixed to 13 October 2026.</sub>

## A small workflow, deliberately simple

| What you can do | How it works in this release |
|:--|:--|
| **See the week ahead** | Read upcoming fictional assignments in the browser dashboard. |
| **Mark work complete** | Move items to the completed view and retain local state after a reload. |
| **Keep a reminder** | Add a local note in the browser version. |
| **Separate the children** | Switch between independent fictional child views. |
| **Make it recognizable** | Change subject colors and keep those preferences locally. |

There are two actual interfaces in the source: the **offline browser dashboard** and an **Android Kotlin/Compose prototype**. Their capabilities are not identical; the Android demo focuses on homework, while the browser also provides notes.

## Open the browser demo

**No server, account, school credentials, or API key is required.**

```bash
git clone https://github.com/jedufa/untis-homework.git
cd untis-homework
```

Open [`docs/index.html`](docs/index.html) from your downloaded checkout in **Chromium**. Try completing a task, switching between Alex and Robin, adding a reminder, and reloading. Browser state persists locally. Settings can reset the demo without clearing unrelated browser data.

**This is a local file demo, not a hosted school service.** It neither retrieves nor updates real homework.

## What's included

| Surface | Present in the source | Release boundary |
|:--|:--|:--|
| **Browser** | Dashboard, local adapter and fixed fictional fixtures | Offline demonstration |
| **Android** | Kotlin/Compose homework views, archive, swipe completion, child/subject appearance | Debug builds only; no published APK |
| **Python** | Normalization, completion, notes and appearance helpers | Local utilities and tests |
| **Swift** | Model, codec and color references | Porting references only; **not** a buildable iOS app |

Live school integrations, account/password handling, production gateways, deployment, an iOS app, private development history and unverified artwork are **not included**.

## Build and validate

The project includes browser tests, Python tests and an Android test/build path:

```bash
python3 -m unittest discover -s tests
npm ci --ignore-scripts
npx playwright install chromium
npm run test:browser

# With Java 17 and Android SDK/platform 36 installed:
cd app/mobile/android
./gradlew testDebugUnitTest assembleDebug
```

The source-release candidate passed **18 Python tests, 13 Android JVM tests and two browser workflows**. Checks included local persistence, child separation, 320/390-pixel layouts and network-call guards. See [validation and its limitations](docs/validation.md) for details.

No APK, IPA or other installable release binary is distributed here.

## Privacy, independence and project status

The browser keeps data in ordinary browser storage; Android uses local preferences. **This is not encrypted storage.** Notes and imported pictures remain on the device, and clearing browser/app storage can remove them. [Read the security notes](SECURITY.md).

This repository is **experimental and unmaintained**. There is no commitment to support, security updates, issue review or future releases. Fork it, adapt the family workflow, or build your own authorized school integration after reviewing the relevant security requirements and terms.

[**Validation**](docs/validation.md) · [**Security**](SECURITY.md) · [**License**](LICENSE) · [**Third-party notices**](THIRD_PARTY_NOTICES.md)

First-party code, tests and documentation are available under the **MIT License**. Untis and WebUntis names and trademarks remain with their respective owners.

---

<div align="center">

### Less hunting for homework. More room for the rest of the day.

*An offline idea, ready for someone to take further.*

</div>
