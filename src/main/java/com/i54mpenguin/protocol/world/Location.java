package com.i54mpenguin.protocol.world;

import lombok.*;

@ToString
@EqualsAndHashCode
@AllArgsConstructor
public class Location {

    @Getter
    @Setter
    private double x, y, z;

}
