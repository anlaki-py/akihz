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
uniform float outlineRadius;
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
float linearChannel(float c) {
    return c <= 0.04045 ? c / 12.92 : pow((c + 0.055) / 1.055, 2.4);
}
float luminance(half3 c) {
    return dot(float3(linearChannel(c.r), linearChannel(c.g), linearChannel(c.b)),
        float3(0.2126, 0.7152, 0.0722));
}
float inkAt(float2 p) {
    if (automaticContrast < 0.5) return fixedInk;
    float2 region = regions[0];
    for (int i = 0; i < $itemCount; ++i) {
        if (abs(p.x - regions[i].x) <= regions[i].y * 0.5) region = regions[i];
    }
    // Average the rendered glass under a whole tab so individual bright pixels
    // cannot flip separate parts of one label between black and white.
    float average = 0;
    for (int y = -1; y <= 1; ++y) {
        for (int x = -1; x <= 1; ++x) {
            float2 samplePoint = float2(region.x + float(x) * region.y * 0.3,
                dimensions.y * (0.5 + float(y) * 0.2));
            average += luminance(atlas.eval(samplePoint).rgb);
        }
    }
    average /= 9.0;
    // Pick the higher WCAG contrast ratio between black and white.
    return (average + 0.05) / 0.05 >= 1.05 / (average + 0.05) ? 0.0 : 1.0;
}
half4 main(float2 coordinate) {
    // Render the full atlas height, then halve the output layer's height. This keeps
    // the glyph input inside the filter's requested bounds instead of being culled.
    float2 p = float2(coordinate.x, coordinate.y * 0.5);
    if (p.y >= dimensions.y) return half4(0);
    half4 background = atlas.eval(p);
    half alpha = glyph(p);
    half outline = max(max(glyph(p + float2(outlineRadius, 0)), glyph(p - float2(outlineRadius, 0))),
        max(glyph(p + float2(0, outlineRadius)), glyph(p - float2(0, outlineRadius))));
    // Empty pixels need no contrast sampling.
    if (max(alpha, outline) < 0.001) return background;
    half ink = half(inkAt(glyphPosition(p)));
    background.rgb = mix(background.rgb, half3(1.0 - ink), max(outline - alpha, 0.0) * 0.35);
    return half4(mix(background.rgb, half3(ink), alpha), 1.0);
}
"""
}
