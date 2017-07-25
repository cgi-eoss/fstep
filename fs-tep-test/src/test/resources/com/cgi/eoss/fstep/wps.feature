Feature: FS-TEP :: WPS

  Scenario: GetCapabilities
    Given FS-TEP WPS is available
    And the default services are loaded
    When a user requests GetCapabilities from WPS
    Then they receive the FS-TEP service list
