# ProfileServerRedirect

This plugin handles automatic server redirection for players based on profile selection using MMOProfiles.

It supports both newly created profiles and existing ones, redirecting players to the correct server after detecting their status using PlaceholderAPI.

NOTE: It doesn't support custom profile names due to the use of placeholders to detect profile selection (in proxy mode profile selection event doesn't work)

## Features

- Sends players to a different server upon profile creation or selection
- Freezes and hides players until a profile is selected
- Uses %mmoprofiles_current_profile_name% to detect profile state
- Optional permission bypass (`profileswitch.bypass`)
- Fully configurable delays and settings via `config.yml`

## Configuration (`config.yml`)

```yaml
profile-prefix: "Profile N"            # Placeholder profile prefix to detect
server-on-profile-create: "lobby"      # Server to send player after profile creation
server-on-profile-select: "RPG"        # Server to send player when selecting existing profile
kick-message: "Server is full!"        # Message shown if kicked
profile-create-delay: 40               # Delay (ticks) before redirect on profile create
profile-select-delay: 100              # Cooldown (ticks) to prevent repeated redirects
kick-delay: 100                        # Delay (ticks) before kicking player
```

## Permissions

- `profileswitch.bypass`: Skips redirection and allows normal login/movement



