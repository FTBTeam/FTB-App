name: Bug Report
description: Report an issue with the Windows version of the FTB App
title: "[Bug]: "
labels: [bug]
body:
  - type: dropdown
    id: os
    validations:
        required: true
    attributes:
      label: What Operating System
      multiple: false
      options:
        - Windows 10
        - Windows 8.1
        - Windows 7
        - Windows Insider Preview
        - MacOS (Latest)
        - MacOS (Old)
        - MacOS (Beta)
        - Linux (Debian)
        - Linux (Arch)
        - Linux (Other)
  - type: input
    id: appversion
    attributes:
      label: App Version
      description: |
        What version of the app are you having issues with?
        You can find the app version at the bottom of the settings page in the app
      placeholder: ex. 202103260308-5fe58412e0-prototyping
    validations:
      required: false
  - type: input
    id: uiversion
    attributes:
      label: UI Version
      description: |
        You can find the UI version at the bottom of the settings page in the app
      placeholder: ex. 4b4de71d
    validations:
      required: false
  - type: textarea
    id: logs
    attributes:
      label: Log Files
      description: "Link(s) to any log files that may help debug your issue"
      placeholder: https://pste.ch/dotedubine
    validations:
      required: false
  - type: input
    id: debugcode
    attributes:
      label: Debug Code
      description: |
        Please run the following tool and provide the code given.
        [Windows](https://dist.creeper.host/tools/ftb-debug/ftb-debug.exe) [MacOS](https://dist.creeper.host/tools/ftb-debug/ftb-debug-macos) [Linux](https://dist.creeper.host/tools/ftb-debug/ftb-debug-linux)
      placeholder: FTB-DBGTEVOISQIRA
    validations:
      required: true
  - type: input
    id: bugdesc
    attributes:
      label: Describe the bug
      description: |
        A clear and concise description of what the bug is
    validations:
      required: true
  - type: textarea
    id: reproduce
    attributes:
      label: Steps to reproduce
      description: Tell us how we can reproduce the issue
      placeholder: |
        1. Go to ...
        2. Click on ...
        3. Scroll down to ...
        4. See error
    validations:
      required: true
  - type: input
    id: expectedBehaviour
    attributes:
      label: Expected behaviour
      description: |
        A clear and concise description of what you expected to happen.
    validations:
      required: true
  - type: textarea
    id: screenshots
    attributes:
      label: Screenshots
      description: If applicable, add screenshots to help explain your problem.
    validations:
      required: false
  - type: textarea
    id: additional
    attributes:
      label: Additional information
      description: Add any other context about the problem here
    validations:
      required: false
  - type: checkboxes
    id: terms
    attributes:
      label: Information
      options:
        - label: I have provided as much information as possible
          required: true
