JLSL
====

Bored of programming your shaders with GLSL, where you're really prompt to make tons of mistakes?

With JLSL, you're now able to program them directly with Java, and fix those problems!


Description:
====
JLSL is a library that will convert Java code into GLSL source code, that way one can create shaders without even needing to learn the specificities of the GLSL language!


Why does it exist ?
====
This library allows a developer who doesn't know (or want) how to program in GLSL to still be able to create shaders for his application.


How it works:
====
Once you give a class to convert to JLSL, the bytecode of that class is extracted.
Then, while reading this bytecode, JLSL will reconstruct a GLSL source code from it, kind of like a decompiler would reconstruct a Java source code.


How to use:
====
First create a FragmentShader subclass or a VertexShader subclass.
Exemple:

```java
package org.jglrxavpok.jlsl;

import org.jglrxavpok.jlsl.GLSL.Uniform;

@GLSL.Extensions({"GL_ARB_explicit_uniform_location"})
public class TestShader extends FragmentShader
{

	@Uniform
	private Vec2 screenSize;
	
	public static final double PI = 3.141592653589793D;
	
	@Override
	public void main()
	{
		Vec4 v = new Vec4(gl_FragCoord.x/screenSize.x,gl_FragCoord.y/screenSize.y,gl_FragCoord.z,gl_FragCoord.w);
		v = normalizer(v, v.length());
		Mat2 testMatrix = new Mat2(new Vec2(v.x, v.y), new Vec2(0,1));
		gl_FragColor = v;
	}

	private Vec4 normalizer(Vec4 v, double l)
	{
		double x1 = v.x/l;
		double y1 = v.y/l;
		double z1 = v.z/l;
		double w1 = v.w/l;
		return new Vec4(x1,y1,z1,w1);
	}

}
```
Note: some of the code in this exemple makes no sense (e.g. use of normalizer although Vec4 has a normalize() method), it is just to test the maximum cases with one file.
* The @Extensions annotation allows you to specify an array of extensions used.
* The @Uniform annotation used on a field make this field translated as an uniform. (Same for @In @Out @Attribute)
* Vec4, Vec3, Vec2, Mat4, Mat3 and Mat2 are replaced by their corresponding type in GLSL

Then, get a String of the translated version:
```java
JLSL.translateToGLSL(YourShaderClass.class, 120);
```
* You can also use byte[] or InputStreams to translate the class.
* 120 is the version of GLSL

After translating it, the resulted shader code:
```glsl
#version 120
#extension GL_ARB_explicit_uniform_location : enable
uniform vec2 screenSize;
#define PI 3.141592653589793

vec4 normalizer(vec4 v, float l)
{
    float x1 = v.x/l;
    float y1 = v.y/l;
    float z1 = v.z/l;
    float w1 = v.w/l;
    return vec4(x1, y1, z1, w1);
}

void main()
{
    vec4 v = vec4(gl_FragCoord.x/screenSize.x, gl_FragCoord.y/screenSize.y, gl_FragCoord.z, gl_FragCoord.w);
    v = normalizer(v, length(v));
    mat2 testMatrix = mat2(vec2(v.x, v.y), vec2(0, 1));
    gl_FragColor = v;
}

```
