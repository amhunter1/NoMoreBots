# NoMoreBots

A powerful anti-bot verification plugin for Velocity proxy servers using LimboAPI with hybrid chat + movement verification system.

## Features

- **Hybrid Chat + Movement Verification**: Players complete both code typing and multi-directional movement challenges
- **Multi-Direction Movement System**: Configurable movement verification (up, down, left, right) with customizable sequences
- **Intelligent Cooldown System**: User + IP based cooldown to prevent repeat verification for legitimate players
- **Timeout Protection**: Configurable response timeout with automatic kick for non-responsive connections
- **Limbo Integration**: Uses LimboAPI to hold unverified players in a virtual limbo world
- **Database Tracking**: SQLite database to track player verification status, attempts, and cooldown periods
- **Configurable Settings**: Highly customizable verification parameters, directions, timeouts, and cooldown settings
- **Multi-language Support**: English and Turkish language files with proper localization
- **Admin Commands**: Administrative tools for managing verification system
- **Attempt Limiting**: Configurable maximum verification attempts before timeout action
- **Advanced IP Tracking**: Prevents same IP different user and same user different IP bypassing

## Requirements

- Velocity 3.3.0+
- LimboAPI plugin
- Java 17+

## Installation

1. Download the latest release from the [releases page](https://github.com/amhunter1/NoMoreBots/releases)
2. Install LimboAPI on your Velocity server
3. Place `nomorebots-1.0.0-SNAPSHOT.jar` in your Velocity plugins folder
4. Restart your server
5. Configure the plugin in `plugins/nomorebots/config.yml`

## Configuration

The plugin creates a configuration file at `plugins/nomorebots/config.yml` with customizable:

### Verification System
- **Chat Verification**: Code length, characters, case sensitivity
- **Movement Verification**: Multi-directional sequence configuration (up:2, left:3, etc.)
- **Direction Angles**: Precise angle ranges for each direction (up, down, left, right)
- **Response Timeout**: Maximum time players have to respond (with kick option)
- **Tolerance Settings**: Angle tolerance for movement detection

### Cooldown System
- **Track by User**: Remember verification by username
- **Track by IP**: Remember verification by IP address  
- **Duration**: How long verification is remembered (24 hours default)
- **Combined Tracking**: Require both same user AND same IP for bypass

### Database & Performance
- SQLite database configuration
- Async database operations
- Session management and cleanup
- Player data caching

### Example Movement Configuration
```yaml
movement:
  directions:
    - "up:2"    # Look up for 2 seconds
    - "left:2"  # Then look left for 2 seconds
    - "down:1"  # Then look down for 1 second
  
  response-timeout: 20 # Max 20 seconds to respond
  kick-on-timeout: true # Kick if timeout exceeded
```

### Example Cooldown Configuration
```yaml
cooldown:
  track-by-user: true  # Track by username
  track-by-ip: true    # Track by IP address
  duration: 86400      # 24 hours cooldown
```

## Commands

- `/nomorebots reload` - Reload plugin configuration
- `/nomorebots stats` - View verification statistics
- `/nomorebots bypass <player>` - Bypass verification for a player
- `/nomorebots verify <player>` - Manually verify a player
- `/nomorebots reset <player>` - Reset player verification status
- `/nomorebots timeout <player> <duration>` - Set timeout for a player

## How It Works

1. **Player Joins**: New players are automatically sent to limbo for verification
2. **Chat Challenge**: Players must type a randomly generated code exactly as shown
3. **Movement Challenge**: Players complete a sequence of directional movements (configured in config)
4. **Cooldown Period**: Successfully verified players are remembered for 24 hours (configurable)
5. **Smart Tracking**: System tracks both username and IP to prevent bypass attempts

### Verification Process
```
Player Joins → Limbo → Chat Code → Movement Sequence → Success → Main Server
                ↓
         (Timeout/Fail → Kicked/Timed Out)
```

### Cooldown Logic
- **Same user, same IP**: Skip verification (if both tracking enabled)
- **Same user, different IP**: Require verification (prevents account sharing)
- **Different user, same IP**: Require verification (prevents IP spoofing)
- **New user/IP combination**: Always require verification

## Building

```bash
git clone https://github.com/amhunter1/NoMoreBots.git
cd NoMoreBots
mvn clean package
```

## Advanced Features

### Multi-Direction Movement
Configure complex movement patterns:
```yaml
directions:
  - "up:2"      # Look up for 2 seconds
  - "right:1"   # Look right for 1 second  
  - "down:3"    # Look down for 3 seconds
  - "left:2"    # Look left for 2 seconds
```

### Precise Angle Control
Fine-tune detection angles:
```yaml
angles:
  up:
    pitch-min: -90.0  # Straight up
    pitch-max: -30.0  # Minimum up angle
  down:
    pitch-min: 30.0   # Minimum down angle
    pitch-max: 90.0   # Straight down
```

### Advanced Cooldown Options
```yaml
cooldown:
  track-by-user: true   # Remember by username
  track-by-ip: true     # Remember by IP
  duration: 86400       # 24 hours in seconds
```

## Performance

- **Async Database Operations**: Non-blocking database queries
- **Player Data Caching**: Reduces database load
- **Session Management**: Automatic cleanup of expired sessions
- **Configurable Timeouts**: Prevents resource waste from inactive connections

## Author

Created by **Melut** ([@amhunter1](https://github.com/amhunter1))

## License

This project is licensed under the MIT License - see the LICENSE file for details.