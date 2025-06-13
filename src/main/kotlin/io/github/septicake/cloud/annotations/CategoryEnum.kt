package io.github.septicake.cloud.annotations

enum class CategoryEnum(val categoryName: String, val description: String) {
    INFORMATION(
        "Information",
        "Commands that provide general information of some kind"
    ),
    HELP(
        "Help",
        "Commands that provide hopefully useful information"
    ),
    DEV(
        "Dev",
        "Dev commands, paws off"
    ),
    TICKET(
        "Ticket",
        "Commands used for tickets"
    ),
    SETUP(
        "Setup",
        "Commands used in the setting up of the bot"
    ),
    MANAGEMENT(
        "Management",
        "Commands used for general bot oversight"
    ),
    TEST(
        "Test",
        "...It's one test command"
    )
}