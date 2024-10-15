# Feature: Check Notification Icon
#   Notification icon should be visible when there are notifications.

#   Scenario: Browsing as non-user
#     When I haven't login
#     Then I should not see "Search" icon in the Bazaar's topbar

#   Scenario: Browsing as existing user "old_learner"
#     When I have logged in as "old_learner"
#     Then I should see "Search" icon in the Bazaar's topbar
#     And I should see "Notification" icon in the Bazaar's topbar
