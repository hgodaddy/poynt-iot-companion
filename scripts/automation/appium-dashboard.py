#!/usr/bin/env python3
"""Optional Appium smoke: tap Refresh on the companion dashboard. Needs APPIUM_URL."""
from __future__ import annotations

import os
import sys

PKG = "co.poynt.cloudmessaging.iot.test"
ACTIVITY = ".ui.DashboardActivity"
PREFIX = f"{PKG}:id/"


def main() -> int:
    url = os.environ.get("APPIUM_URL")
    if not url:
        print("APPIUM_URL is not set; skip Appium dashboard smoke", file=sys.stderr)
        return 2
    try:
        from appium import webdriver
        from appium.options.android import UiAutomator2Options
        from appium.webdriver.common.appiumby import AppiumBy
    except ImportError:
        print("Install Appium Python client: pip install Appium-Python-Client", file=sys.stderr)
        return 2

    options = UiAutomator2Options()
    options.platform_name = "Android"
    options.app_package = PKG
    options.app_activity = ACTIVITY
    options.no_reset = True
    driver = webdriver.Remote(url, options=options)
    try:
        driver.find_element(AppiumBy.ID, PREFIX + "dashboard_root")
        driver.find_element(AppiumBy.ID, PREFIX + "btn_refresh").click()
        driver.find_element(AppiumBy.ID, PREFIX + "btn_full_flow")
        print("Appium dashboard ids ok")
        return 0
    finally:
        driver.quit()


if __name__ == "__main__":
    sys.exit(main())
