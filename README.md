# NoMoreBots

A powerful anti-bot verification plugin for Velocity proxy servers using LimboAPI.

## Features

- **Interactive GUI Verification**: Players complete item selection challenges in a custom inventory GUI
- **Limbo Integration**: Uses LimboAPI to hold unverified players in a virtual limbo world
- **Database Tracking**: SQLite database to track player verification status and attempts
- **Configurable Settings**: Customizable verification parameters, items, and messages
- **Multi-language Support**: English and Turkish language files included
- **Admin Commands**: Administrative tools for managing verification system
- **Attempt Limiting**: Configurable maximum verification attempts before action

## Requirements

- Velocity 3.3.0+
- LimboAPI plugin
- Protocolize plugin
- Java 17+

## Installation

1. Download the latest release from the [releases page](https://github.com/amhunter1/NoMoreBots/releases)
2. Install LimboAPI and Protocolize on your Velocity server
3. Place `nomorebots-1.0.0-SNAPSHOT.jar` in your Velocity plugins folder
4. Restart your server
5. Configure the plugin in `plugins/nomorebots/config.yml`

## Configuration

The plugin creates a configuration file at `plugins/nomorebots/config.yml` with customizable:
- Verification GUI settings
- Target and random items for challenges
- Maximum attempts and timeout settings
- Database configuration
- Language preferences

## Commands

- `/nomorebots reload` - Reload plugin configuration
- `/nomorebots stats` - View verification statistics
- `/nomorebots bypass <player>` - Bypass verification for a player

## Building

```bash
git clone https://github.com/amhunter1/NoMoreBots.git
cd NoMoreBots
mvn clean package
```

## Author

Created by **Melut** ([@amhunter1](https://github.com/amhunter1))

## License

This project is licensed under the MIT License - see the LICENSE file for details.