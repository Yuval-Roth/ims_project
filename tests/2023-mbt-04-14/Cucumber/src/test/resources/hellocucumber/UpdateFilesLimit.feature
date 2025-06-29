Feature: add and remove participants

  Scenario Outline: Admin adds new participant
    Given A logged in admin user with username and password
    And There is no participant with the email "<email>"
    When Add participant with the email "<email>"
    Then The participant with email "<email>" added successfully
    Examples:
      | email                   |
      | tamirosh@post.bgu.ac.il |

  Scenario Outline: Admin remove participant
    Given A logged in admin user with username and password
    And There is a participant with the email "<email>"
    When remove the participant with the email "<email>"
    Then The participant with email "<email>" removed successfully
    Examples:
      | email                   |
      | tamirosh@post.bgu.ac.il |

  Scenario Outline: Admin adds new operator
    Given A logged in admin user with username and password
    And There is no operator with the username "<username>"
    When Add operator with the username "<username>" and password "<password>"
    Then The operator with username "<username>" added successfully
    And The operator with username "<username>" and password "<password>" can log in
    Examples:
      | username | password |
      | tamirosh | Pass1234 |

  Scenario Outline: Admin removes operator
    Given A logged in admin user with username and password
    And There is an operator with the username "<username>" and password "<password>"
    When Remove the operator with the username "<username>"
    Then The operator with username "<username>" removed successfully
    And The operator with username "<username>" and password "<password>" can't log in
    Examples:
      | username | password |
      | tamirosh | Pass1234 |

  Scenario Outline: Operator can see Lobbies and Session Data functionality
    Given Exists an operator with the username "<username>" and password "<password>"
    And The operator with the username "<username>" and password "<password>" is logged in
    When The operator is in the main menu page
    Then The operator can see the Lobbies and Session Data buttons
    Examples:
      | username | password |
      | tamirosh | Pass1234 |

  Scenario Outline: Operator can't see Participants and Operators functionality
    Given Exists an operator with the username "<username>" and password "<password>"
    And The operator with the username "<username>" and password "<password>" is logged in
    When The operator is in the main menu page
    Then The operator can't see the Participants and Operators buttons
    Examples:
      | username | password |
      | tamirosh | Pass1234 |

  Scenario Outline: Admin creates a lobby
    Given A logged in admin user with username and password
    And There is a participant with the email "<email1>" and name "<firstname1>" "<lastname1>"
    And There is a participant with the email "<email2>" and name "<firstname2>" "<lastname2>"
    When Creates a lobby with the participants "<firstname1>" "<lastname1>" and "<firstname2>" "<lastname2>"
    Then The lobby with the participants "<firstname1>" "<lastname1>" and "<firstname2>" "<lastname2>" created successfully
    Examples:
      | email1                  | email2         | firstname1 | firstname2 | lastname1 | lastname2 |
      | tamirosh@post.bgu.ac.il | tamir@mail.com | Tamir      | Oshri      | Avisar    | Avisar    |

  Scenario Outline: Admin removes a lobby
    Given A logged in admin user with username and password
    And There is a participant with the email "<email1>" and name "<firstname1>" "<lastname1>"
    And There is a participant with the email "<email2>" and name "<firstname2>" "<lastname2>"
    When Exists a lobby with the participants "<firstname1>" "<lastname1>" and "<firstname2>" "<lastname2>"
    Then The lobby with the participants "<firstname1>" "<lastname1>" and "<firstname2>" "<lastname2>" can removed successfully
    Examples:
      | email1                  | email2         | firstname1 | firstname2 | lastname1 | lastname2 |
      | tamirosh@post.bgu.ac.il | tamir@mail.com | Tamir      | Oshri      | Avisar    | Avisar    |