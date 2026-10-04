package io.github.jamisuni.tangram.play.draw

// decision DA-92: there is no dp scope any more. The earlier `inDp` ran a drawing block under `scale(density)`, which
// the API 26 hardware renderer blurs for every path (measured: stroked, dashed and filled paths blur by about 2.5 px
// each side; rects, round rects and circles do not). The rule now lives in the header of PlayDrawing.kt: dp values are
// multiplied by `density` where they are used and drawn in px (G-03: the one dp to px step is that multiplication).
