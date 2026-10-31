package akihz.anlaki.dev.presentation.glass

// The capsule normal bends samples inward near the rim. Keep this independent of
// foreground drawing so labels and touch targets never distort or blur.
internal const val NavbarGlassShader = """
uniform shader backdrop;
uniform float2 dimensions;
uniform float refraction;
uniform float edgeWidth;
uniform float dispersion;
uniform float blurRadius;
uniform float tintOpacity;
layout(color) uniform half4 tint;
uniform float saturation;
uniform float brightness;
uniform float highlight;
uniform float2 lightDirection;

half4 sampleAt(float2 p) {
    return backdrop.eval(clamp(p, float2(0.5), dimensions - float2(0.5)));
}
half4 softened(float2 p) {
    if (blurRadius < 0.01) return sampleAt(p);
    half4 c = sampleAt(p) * 0.25;
    c += sampleAt(p + float2(blurRadius, 0)) * 0.125;
    c += sampleAt(p - float2(blurRadius, 0)) * 0.125;
    c += sampleAt(p + float2(0, blurRadius)) * 0.125;
    c += sampleAt(p - float2(0, blurRadius)) * 0.125;
    c += sampleAt(p + float2(blurRadius, blurRadius)) * 0.0625;
    c += sampleAt(p - float2(blurRadius, blurRadius)) * 0.0625;
    c += sampleAt(p + float2(blurRadius, -blurRadius)) * 0.0625;
    c += sampleAt(p + float2(-blurRadius, blurRadius)) * 0.0625;
    return c;
}
half4 main(float2 p) {
    float radius = dimensions.y * 0.5;
    float2 center = dimensions * 0.5;
    float2 axis = float2(clamp(p.x, radius, dimensions.x - radius), center.y);
    float2 delta = p - axis;
    float distanceToAxis = length(delta);
    float2 normal = delta / max(distanceToAxis, 0.001);
    float edge = 1.0 - smoothstep(0.0, edgeWidth, radius - distanceToAxis);
    float2 bent = p - normal * refraction * edge * edge;
    half4 c = softened(bent);
    if (dispersion > 0.01) {
        c.r = softened(bent - normal * dispersion * edge).r;
        c.b = softened(bent + normal * dispersion * edge).b;
    }
    half luma = dot(c.rgb, half3(0.2126, 0.7152, 0.0722));
    c.rgb = mix(half3(luma), c.rgb, half(saturation)) * half(brightness);
    c.rgb = mix(c.rgb, tint.rgb, half(tintOpacity));
    float reflection = pow(edge, 3.0) * (0.25 + 0.75 * max(dot(normal, lightDirection), 0.0));
    c.rgb += half(highlight * reflection);
    return half4(clamp(c.rgb, 0.0, 1.0), 1.0);
}
"""
