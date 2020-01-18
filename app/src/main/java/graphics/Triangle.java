package graphics;

import android.graphics.RectF;

import static android.opengl.GLES20.GL_ARRAY_BUFFER;
import static android.opengl.GLES20.GL_ELEMENT_ARRAY_BUFFER;
import static android.opengl.GLES20.GL_FLOAT;
import static android.opengl.GLES20.GL_STATIC_DRAW;
import static android.opengl.GLES20.GL_TEXTURE0;
import static android.opengl.GLES20.GL_TEXTURE_2D;
import static android.opengl.GLES20.GL_TRIANGLES;
import static android.opengl.GLES20.GL_UNSIGNED_INT;
import static android.opengl.GLES20.glActiveTexture;
import static android.opengl.GLES20.glBindBuffer;
import static android.opengl.GLES20.glBindTexture;
import static android.opengl.GLES20.glBufferData;
import static android.opengl.GLES20.glDisableVertexAttribArray;
import static android.opengl.GLES20.glDrawElements;
import static android.opengl.GLES20.glEnableVertexAttribArray;
import static android.opengl.GLES20.glGenBuffers;
import static android.opengl.GLES20.glGetAttribLocation;
import static android.opengl.GLES20.glGetUniformLocation;
import static android.opengl.GLES20.glUniform1i;
import static android.opengl.GLES20.glUniform4fv;
import static android.opengl.GLES20.glUniformMatrix4fv;
import static android.opengl.GLES20.glUseProgram;
import static android.opengl.GLES20.glVertexAttribPointer;

public class Triangle extends RenderObject {

    private static final int POSITION_COMPONENT_COUNT = 4;
    private static final int TEXCOORD_COMPONENT_COUNT = 2;
    //3 indices per triangle
    private int[] indices = {0,1,2};//always 0,1,2

    //pass vertices through to gl_position
    public String vertexShaderSource =
                            "attribute vec4 a_Position;" +
                    "attribute vec2 a_TextureCoordinates;" +
                    "varying vec2 v_TextureCoordinates;" +
                    "uniform mat4 mvpMatrix;"+
                    "void main(){" +
                    "gl_Position = mvpMatrix*a_Position;" +
                    "v_TextureCoordinates = a_TextureCoordinates;" +
                    "}";
    //pass a single uniform color to the fragment shader
    public String fragmentShaderSource =
                    "precision mediump float;" +
                    "uniform vec4 u_FragColor;" +
                    "uniform sampler2D u_Texture;" +
                    "varying vec2 v_TextureCoordinates;" +
                    "void main(){" +
                    "gl_FragColor = texture2D(u_Texture,v_TextureCoordinates);" +//does not use the color to alter fragcolor
                    "}";

    private float[] color = {1f,0.2f,0.5f,1f};
    //vertices defining a triangle in normalized device coordinates
    private float[] triangleVerts = {
            0f, 1f,0f,1f, //top
            -1f,-1f,0f,1f,//left
            1f,-1f,0f,1f//right
    };

    private float[] textureCoordinates = {
            //one texture coordinate for each point
            //gl will stretch or repeat the texture depending on what i tell it to do
            //texture coordinates go from bottom-left(0,0)-> (1,1)top-right
            0f,0f, //top -left
            1f,1f, //lower left
            1f,0f, //lower right

    };

    public Triangle(float[] vertices,float[] color){
        initIndexBuffer(indices);
        initVertexBuffer(vertices);
        initColorBuffer(color);
    }

    public Triangle(float[] vertices, float[] color,float[] textureCoordinates){
        initIndexBuffer(indices);
        initVertexBuffer(vertices);
        initColorBuffer(color);
        initTextureBuffer(textureCoordinates);
    }

    public Triangle(RectF boundingBox) {
        //center the triangle in the bounding box
        float[] vertices = new float[]{
                //x,y,z,1
                //top-left
                boundingBox.left,boundingBox.top,0f,1f,
                //bottom-left
                boundingBox.left,boundingBox.bottom,0f,1f,
                //bottom-right
                boundingBox.right,boundingBox.bottom,0f,1f
        };
        initVertexBuffer(vertices);
        initColorBuffer(color);
        initIndexBuffer(indices);
    }

    @Override
    public void setAttributeAndVBO() {
        buffers = new int[2];
        // buffers = new int[3];//one index buffer and one vertex buffer
        //create memory on the gpu to store vertices
        //colors as uniform's must be passed from cpu to gpu on every drawcall
        glGenBuffers(buffers.length,buffers,0);//num buffers,array for resulting bufferids, offset into array to place ids
        //vao well yes but actually no glgenvertexarrays is not part of opengl es 2 api
        glBindBuffer(GL_ELEMENT_ARRAY_BUFFER,buffers[0]);//now all glbufferdata calls will effect this buffer
        //
        glBufferData(GL_ELEMENT_ARRAY_BUFFER,indexBuffer.capacity()*BYTES_PER_INT,indexBuffer,GL_STATIC_DRAW);//
        //vbo
        glBindBuffer(GL_ARRAY_BUFFER,buffers[1]);
        glBufferData(GL_ARRAY_BUFFER,vertexBuffer.capacity()*BYTES_PER_FLOAT,vertexBuffer,GL_STATIC_DRAW);//
        //vbo for texture coordinates
        //glBindBuffer(GL_ARRAY_BUFFER,buffers[2]);
        //glBufferData(GL_ARRAY_BUFFER,textureBuffer.capacity()*BYTES_PER_FLOAT,textureBuffer,GL_STATIC_DRAW);//

        positionReference = glGetAttribLocation(program,"a_Position");
        //texCoordinateReference = glGetAttribLocation(program,"a_TextureCoordinates");//coordinates that will be sent as varying to fragment shader
        //texDataReference = glGetUniformLocation(program,"u_Texture");//the texture data in the gpu
        colorReference = glGetUniformLocation(program,"u_FragColor");
        mvpMatrixReference = glGetUniformLocation(program,"mvpMatrix");
    }

    @Override
    public void draw() {
        //draw all shapes here
        glUseProgram(program);
        //enable the vertex attributes
        glBindBuffer(GL_ELEMENT_ARRAY_BUFFER,buffers[0]);//indices
        glBindBuffer(GL_ARRAY_BUFFER,buffers[1]);//vertices

        glVertexAttribPointer(positionReference,POSITION_COMPONENT_COUNT,GL_FLOAT,false,POSITION_COMPONENT_COUNT*BYTES_PER_FLOAT,0);
        glEnableVertexAttribArray(positionReference);//enable the per vertex attribute

        //glActiveTexture(GL_TEXTURE0);///activate this texture at position 0
        //glBindTexture(GL_TEXTURE_2D, texDataReference);//the texture data is of type gl_texture2d
        //glBindBuffer(GL_ARRAY_BUFFER,buffers[2]);//vertices
        //glVertexAttribPointer(texCoordinateReference,TEXCOORD_COMPONENT_COUNT,GL_FLOAT,false, TEXCOORD_COMPONENT_COUNT*BYTES_PER_FLOAT,0);
        //glEnableVertexAttribArray(texCoordinateReference);//enable per vertex texture coordinates

        //draw triangles,number of indices to draw,type of index,offset into index buffer
        glUniform4fv(colorReference,1,colorBuffer);
        glUniformMatrix4fv(mvpMatrixReference,1,false,mvpMatrixBuffer);
        //glUniform1i(texDataReference,0);//tell the gpu to use this texture

        glDrawElements(GL_TRIANGLES,3,GL_UNSIGNED_INT,0);//wanna use elements

        glDisableVertexAttribArray(positionReference);
        //glDisableVertexAttribArray(texCoordinateReference);
    }


}
