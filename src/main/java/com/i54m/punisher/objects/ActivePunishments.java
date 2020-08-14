package com.i54m.punisher.objects;

import lombok.*;

@AllArgsConstructor
@NoArgsConstructor
@ToString
public class ActivePunishments {
    @Getter
    @Setter
    private Punishment mute, ban;

    public boolean isNoneActive() {
        return mute == null && ban == null;
    }

    public boolean muteActive() {
        return mute != null;
    }

    public boolean banActive() {
        return ban != null;
    }
}
