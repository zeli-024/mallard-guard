# Sharp 64px parry impact

center-impact.png is the editable 384 x 64 RGBA strip: six 64 x 64 frames.
The first two drawings combine an upright cross and its larger translucent angled layer.
The following four drawings clear the center and disperse into thin, unevenly fading arcs.
There is no third cross or glow. Motion is drawn in the pixels; runtime does not resize the animation over time.

Exports use nearest-neighbor sampling and discrete source alpha, preserving hard pixel silhouettes.
Translucent parts stay translucent without a blurred contour. All frames retain the same origin.
Run bash export-center-impact.sh after editing. This exports the six white frames and six matching tint masks.
The masks select more of the outer artwork and preserve the small white center highlight.
They never add alpha outside the original silhouette. Mask feathering controls tint selection inside artwork, not edge sharpness.

The game uses the tracer Start, Middle and End colors across the impact's normalized lifetime.
The PNG mask alpha controls tint selection; Opacity scales the complete effect.
Size, Spin Speed, Opacity and Duration are the only impact controls.
Starting rotation and spin direction are randomized once per appearance. Duration is both animation time and lifetime.

`preview.gif` shows the six drawings without runtime randomized spin.
