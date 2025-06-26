Feature: add and remove participants

  Scenario Outline: Manager adds new participant
    Given A logged in admin user with username and password
    And There is no participant with the email "<email>"
    When Add participant with the email "<email>"
    Then The participant with email "<email>" added successfully
    Examples:
      | email                   |
      | tamirosh@post.bgu.ac.il |

  Scenario Outline: Manager remove participant
    Given A logged in admin user with username and password
    And There is a participant with the email "<email>"
    When remove the participant with the email "<email>"
    Then The participant with email "<email>" removed successfully
    Examples:
      | email                   |
      | tamirosh@post.bgu.ac.il |

