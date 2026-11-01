package akihz.anlaki.dev.presentation.glass

/** Builds a shader for [itemCount] tab regions, sampling glass and glyphs from a two-row atlas. */
internal fun navigationContentShader(itemCount: Int): String {
    require(itemCount > 0)
    return """
uniform shader atlas;
uniform float2 dimensions;
uniform float4 pillBounds;
uniform float2 outerLens;
uniform float2 pillLens;
uniform float strength;
uniform float automaticContrast;
uniform float fixedInk;
uniform float inks[$itemCount];
uniform float2 regions[$itemCount];

float2 bend(float2 p, float4 bounds, float2 lens) {
    float2 local = p - bounds.xy;
    float radius = bounds.w * 0.5;
    float2 axis = float2(clamp(local.x, radius, bounds.z - radius), radius);
    float2 delta = local - axis;
    float distanceToAxis = length(delta);
    if (distanceToAxis > radius) return p;
    float edge = 1.0 - smoothstep(0.0, lens.y, radius - distanceToAxis);
    return p - delta / max(distanceToAxis, 0.001) * lens.x * edge * edge * strength;
}
float2 glyphPosition(float2 p) {
    float2 q = bend(p, float4(0, 0, dimensions), outerLens);
    return bend(q, pillBounds, pillLens);
}
half glyph(float2 p) {
    float2 q = glyphPosition(p);
    if (q.x < 0 || q.y < 0 || q.x >= dimensions.x || q.y >= dimensions.y) return 0;
    return atlas.eval(q + float2(0, dimensions.y)).a;
}
float inkAt(float2 p) {
    if (automaticContrast < 0.5) return fixedInk;
    float ink = inks[0];
    for (int i = 0; i < $itemCount; ++i) {
        if (abs(p.x - regions[i].x) <= regions[i].y * 0.5) ink = inks[i];
    }
    return ink;
}
half4 main(float2 p) {
    if (p.y >= dimensions.y) return half4(0);
    half4 background = atlas.eval(p);
    half alpha = glyph(p);
    // Empty pixels need no contrast sampling.
    if (alpha < 0.001) return background;
    half ink = half(inkAt(glyphPosition(p)));
    return half4(mix(background.rgb, half3(ink), alpha), 1.0);
}
"""
}
