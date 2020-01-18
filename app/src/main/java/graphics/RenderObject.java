package graphics;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.opengl.GLES11Ext;
import android.opengl.GLES20;
import android.opengl.Matrix;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;
import java.nio.IntBuffer;

import static android.opengl.GLES11Ext.GL_TEXTURE_EXTERNAL_OES;
import static android.opengl.GLES20.GL_COMPILE_STATUS;
import static android.opengl.GLES20.GL_FRAGMENT_SHADER;
import static android.opengl.GLES20.GL_LINK_STATUS;
import static android.opengl.GLES20.GL_NEAREST;
import static android.opengl.GLES20.GL_TEXTURE_2D;
import static android.opengl.GLES20.GL_TEXTURE_MAG_FILTER;
import static android.opengl.GLES20.GL_TEXTURE_MIN_FILTER;
import static android.opengl.GLES20.GL_TRUE;
import static android.opengl.GLES20.GL_VERTEX_SHADER;
import static android.opengl.GLES20.glAttachShader;
import static android.opengl.GLES20.glBindTexture;
import static android.opengl.GLES20.glCompileShader;
import static android.opengl.GLES20.glCreateProgram;
import static android.opengl.GLES20.glCreateShader;
import static android.opengl.GLES20.glDeleteShader;
import static android.opengl.GLES20.glGenTextures;
import static android.opengl.GLES20.glGetProgramInfoLog;
import static android.opengl.GLES20.glGetProgramiv;
import static android.opengl.GLES20.glGetShaderInfoLog;
import static android.opengl.GLES20.glGetShaderiv;
import static android.opengl.GLES20.glLinkProgram;
import static android.opengl.GLES20.glShaderSource;
import static android.opengl.GLES20.glTexParameteri;
import static android.opengl.GLUtils.texImage2D;

/**
 * Created by Ramandeep on 2017-09-05.
 */

public abstract class RenderObject {
    static final int BYTES_PER_FLOAT = 4;
    static final int BYTES_PER_INT = 4;

    IntBuffer indexBuffer; //indices
    FloatBuffer vertexAndTextureBuffer;
    FloatBuffer vertexBuffer; //vertices
    FloatBuffer colorBuffer; //colors ;
    FloatBuffer textureBuffer;//texture coordinates
    FloatBuffer mvpMatrixBuffer;//model-view-projection matrix

    int[] buffers;

    public int program;
    private int vertexShader;
    private int fragmentShader;

    int mvpMatrixReference = -1;
    int positionReference = -1;
    int colorReference = -1;
    int texCoordinateReference = -1;
    int textureTransformReference = -1;
    public int texDataReference = -1;


    public void createProgram(String vertex_basic_source, String fragment_basic_source) {
        setShaderFromSource(GL_VERTEX_SHADER,vertex_basic_source);
        setShaderFromSource(GL_FRAGMENT_SHADER,fragment_basic_source);
        createProgram();
    }
    public void createProgram(Context context, int vertex_basic, int fragment_basic) {
        //read the shaders into strings
        setShaderFromSource(GL_VERTEX_SHADER,readSourceFromRaw(context,vertex_basic));
        setShaderFromSource(GL_FRAGMENT_SHADER,readSourceFromRaw(context,fragment_basic));
        createProgram();
    }
    private void createProgram() {
        program = glCreateProgram();//get an available program id

        glAttachShader(program, vertexShader);
        glAttachShader(program, fragmentShader);
        //"hook up" vertex output to fragment input
        glLinkProgram(program);
        checkProgramLinkStatus(program);
        //shaders can be deleted since program has all the code
        glDeleteShader(vertexShader);
        glDeleteShader(fragmentShader);
    }

    public void checkProgramLinkStatus(int program){
        int [] linkStatus = new int[1];
        glGetProgramiv(program,GL_LINK_STATUS,linkStatus,0);
        if(linkStatus[0] != GL_TRUE){
            System.out.println("Failed to link program.");
            System.out.println(glGetProgramInfoLog(program));
        }else{
            System.out.println("Successfully linked program.");
        }
    }

    public void setShaderFromSource(int shaderType, String shaderSource) {
        switch (shaderType) {
            case GL_VERTEX_SHADER:
                vertexShader = loadShader(GL_VERTEX_SHADER, shaderSource);
                break;
            case GL_FRAGMENT_SHADER:
                fragmentShader = loadShader(GL_FRAGMENT_SHADER, shaderSource);
                break;
        }
    }
    private int loadShader(int type, String shaderSource) {
        int shaderReference = glCreateShader(type);
        //send the source to gl
        glShaderSource(shaderReference, shaderSource);
        //try and compile the shader
        glCompileShader(shaderReference);

        int[] compileResult = new int[1];
        //check for compile error returns GL_FALSE if not compiled
        glGetShaderiv(shaderReference, GL_COMPILE_STATUS, compileResult, 0);
        if (compileResult[0] != GL_TRUE) {
            System.out.println("Failed to compile shader.");
            System.out.println(glGetShaderInfoLog(shaderReference));
            glDeleteShader(shaderReference);
        } else {
            System.out.println("Shader compiled.");
        }
        return shaderReference;
    }

    public void initVertexBuffer(float[] vertices) {
        vertexBuffer = getNativeOrderFloatBuffer(vertices.length);
        vertexBuffer.put(vertices);
        vertexBuffer.position(0);
    }

    public void initColorBuffer(float[] color) {
        colorBuffer = getNativeOrderFloatBuffer(color.length);
        colorBuffer.put(color);
        colorBuffer.position(0);
    }

     public void initIndexBuffer(int[] indices) {
        indexBuffer = getNativeOrderIntBuffer(indices.length);
        indexBuffer.put(indices);
        indexBuffer.position(0);
    }

    public void initTextureBuffer(float[] textureCoordinates){
        textureBuffer = getNativeOrderFloatBuffer(textureCoordinates.length);
        textureBuffer.put(textureCoordinates);
        textureBuffer.position(0);
    }

    public void initVertexTextureBuffer(float[] verticesAndTexture) {
        vertexAndTextureBuffer = getNativeOrderFloatBuffer(verticesAndTexture.length);
        vertexAndTextureBuffer.put(verticesAndTexture);
        vertexAndTextureBuffer.position(0);
    }

    public void setMvpMatrix(float[] mvpMatrix) {
        mvpMatrixBuffer = getNativeOrderFloatBuffer(mvpMatrix.length);
        mvpMatrixBuffer.put(mvpMatrix);
        mvpMatrixBuffer.position(0);
    }

     FloatBuffer getNativeOrderFloatBuffer(int size) {
        ByteBuffer byteBuffer = ByteBuffer.allocateDirect(size * BYTES_PER_FLOAT);
        byteBuffer.order(ByteOrder.nativeOrder());//native byte order
        return byteBuffer.asFloatBuffer();
    }

    IntBuffer getNativeOrderIntBuffer(int size) {
        ByteBuffer byteBuffer = ByteBuffer.allocateDirect(size * BYTES_PER_INT);
        byteBuffer.order(ByteOrder.nativeOrder());//native byte order
        return byteBuffer.asIntBuffer();
    }

    abstract void draw();

     public static void computeOrthoMVP(int width, int height, float near, float far, float[] viewMatrix, float[] projectionMatrix, float[] mvpMatrix) {
        float left = width / 2f;
        float right = -left;
        float bottom = -height / 2f;
        float top = -bottom;
        //                              eye         center   up
        Matrix.setLookAtM(viewMatrix, 0, 0f, 0f, -1f, 0f, 0f, 0f, 0f, 1f, 0f);
        Matrix.orthoM(projectionMatrix, 0, left, right, bottom, top, near, far);
        Matrix.multiplyMM(mvpMatrix, 0, projectionMatrix, 0, viewMatrix, 0);
    }

     public static String readSourceFromRaw(Context context, int resourceId) {
        BufferedReader br = new BufferedReader(new InputStreamReader(context.getResources().openRawResource(resourceId)));
        StringBuilder stringBuilder = new StringBuilder();
        String line;
        try {
            line = br.readLine();
            while (line != null) {
                stringBuilder.append(line+"\n");
                line = br.readLine();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return stringBuilder.toString();
    }

    public abstract void setAttributeAndVBO();

    public void loadTexture(final Context context, final int resourceId,final int textureType)
    {
        final int[] textureHandle = new int[1];

        //get an available texture id
        glGenTextures(1, textureHandle, 0);
        //if texture is type gl_texture2d
        if (textureType == GL_TEXTURE_2D)
        {
            final BitmapFactory.Options options = new BitmapFactory.Options();
            options.inScaled = false;   // No pre-scaling

            // Read in the resource
            final Bitmap bitmap = BitmapFactory.decodeResource(context.getResources(), resourceId, options);

            // Bind to the texture in OpenGL
            glBindTexture(GL_TEXTURE_2D, textureHandle[0]);

            // Set filtering
            //how to fill space if texture needs to be larger or smaller than it is
            glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MIN_FILTER, GL_NEAREST);
            glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MAG_FILTER, GL_NEAREST);

            // Load the bitmap into the bound texture.
            texImage2D(GL_TEXTURE_2D, 0, bitmap, 0);

            // Recycle the bitmap, since its data has been loaded into OpenGL.
            bitmap.recycle();
        }else if(textureType == GL_TEXTURE_EXTERNAL_OES) {
            glBindTexture(GL_TEXTURE_EXTERNAL_OES, textureHandle[0]);
            GLES20.glTexParameteri(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, GLES20.GL_TEXTURE_WRAP_S, GLES20.GL_CLAMP_TO_EDGE);
            GLES20.glTexParameteri(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, GLES20.GL_TEXTURE_WRAP_T, GLES20.GL_CLAMP_TO_EDGE);
            GLES20.glTexParameteri(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, GLES20.GL_TEXTURE_MIN_FILTER, GLES20.GL_NEAREST);
            GLES20.glTexParameteri(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, GLES20.GL_TEXTURE_MAG_FILTER, GLES20.GL_NEAREST);
        }

        if (textureHandle[0] == 0)
        {
            throw new RuntimeException("Error loading texture.");
        }

        texDataReference = textureHandle[0];
    }



}
