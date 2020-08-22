package graphics;

import android.graphics.SurfaceTexture;
import android.renderscript.Matrix4f;

import java.util.Arrays;

import static android.opengl.GLES11Ext.GL_TEXTURE_EXTERNAL_OES;
import static android.opengl.GLES20.GL_ARRAY_BUFFER;
import static android.opengl.GLES20.GL_DYNAMIC_DRAW;
import static android.opengl.GLES20.GL_ELEMENT_ARRAY_BUFFER;
import static android.opengl.GLES20.GL_FLOAT;
import static android.opengl.GLES20.GL_STATIC_DRAW;
import static android.opengl.GLES20.GL_TEXTURE0;
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
import static android.opengl.GLES20.glUniformMatrix4fv;
import static android.opengl.GLES20.glUseProgram;
import static android.opengl.GLES20.glVertexAttribPointer;

public class CameraQuad extends RenderObject {
    private static final int POSITION_COMPONENT_COUNT = 4;
    private static final int TEXCOORD_COMPONENT_COUNT = 2;
    private static final int vertexStride = (POSITION_COMPONENT_COUNT + TEXCOORD_COMPONENT_COUNT)*BYTES_PER_FLOAT;

    private final int[] indices = {
            0,1,2,
            2,3,0};

    private Matrix4f textureTransform;
    //no color parameter
    public CameraQuad(float[] verticesAndTexture) {
        initIndexBuffer(indices);
        initVertexTextureBuffer(verticesAndTexture);
        textureTransform = new Matrix4f();
    }

    @Override
    public void setAttributeAndVBO() {
        buffers = new int[2];//one buffer for index, one for vertex
        //           num, array,offset into array
        glGenBuffers(buffers.length,buffers,0);
        //send indices
        glBindBuffer(GL_ELEMENT_ARRAY_BUFFER,buffers[0]);
        glBufferData(GL_ELEMENT_ARRAY_BUFFER,indexBuffer.capacity()*BYTES_PER_INT,indexBuffer,GL_STATIC_DRAW);
        //send vertices and texture coordinates interleaved
        glBindBuffer(GL_ARRAY_BUFFER,buffers[1]);//
        glBufferData(GL_ARRAY_BUFFER,vertexAndTextureBuffer.capacity()*BYTES_PER_FLOAT,vertexAndTextureBuffer,GL_DYNAMIC_DRAW);
        //grab references for position and color
        positionReference = glGetAttribLocation(program,"aPosition");
        mvpMatrixReference = glGetUniformLocation(program,"mvpMatrix");

        texCoordinateReference = glGetAttribLocation(program,"aTexCoordinates");
        texDataReference = glGetUniformLocation(program,"uCameraTexture");
        textureTransformReference = glGetUniformLocation(program,"uTextureTransform");
    }


    @Override
    public void draw(){
        //draw triangles,6 indices,type,offset
        glUseProgram(program);

        glBindBuffer(GL_ELEMENT_ARRAY_BUFFER,buffers[0]);//bind my index buffer
        glBindBuffer(GL_ARRAY_BUFFER,buffers[1]);//bind my vertex buffer

        glActiveTexture(GL_TEXTURE0);
        glBindTexture(GL_TEXTURE_EXTERNAL_OES,texDataReference);

        glVertexAttribPointer(positionReference,POSITION_COMPONENT_COUNT,GL_FLOAT,false,vertexStride,0);
        glVertexAttribPointer(texCoordinateReference,TEXCOORD_COMPONENT_COUNT,GL_FLOAT,false,vertexStride,POSITION_COMPONENT_COUNT*BYTES_PER_FLOAT);

        glEnableVertexAttribArray(positionReference);
        glEnableVertexAttribArray(texCoordinateReference);

        glUniformMatrix4fv(mvpMatrixReference,1,false,mvpMatrixBuffer);//uniform mvp matrix
        glUniform1i(texDataReference,0);

        glDrawElements(GL_TRIANGLES,indices.length,GL_UNSIGNED_INT,0);

        glDisableVertexAttribArray(positionReference);
        glDisableVertexAttribArray(texCoordinateReference);
    }

    public void drawCamera(SurfaceTexture mSurfaceTexture) {

        //draw triangles,6 indices,type,offset
        glUseProgram(program);

        glBindBuffer(GL_ELEMENT_ARRAY_BUFFER,buffers[0]);//bind my index buffer
        glBindBuffer(GL_ARRAY_BUFFER,buffers[1]);//bind my vertex buffer

        glActiveTexture(GL_TEXTURE0);

        mSurfaceTexture.updateTexImage();
        mSurfaceTexture.getTransformMatrix(textureTransform.getArray());
        //glBindTexture(GL_TEXTURE_EXTERNAL_OES,texDataReference);

        glVertexAttribPointer(positionReference,POSITION_COMPONENT_COUNT,GL_FLOAT,false,vertexStride,0);
        glVertexAttribPointer(texCoordinateReference,TEXCOORD_COMPONENT_COUNT,GL_FLOAT,false,vertexStride,POSITION_COMPONENT_COUNT*BYTES_PER_FLOAT);

        glEnableVertexAttribArray(positionReference);
        glEnableVertexAttribArray(texCoordinateReference);

        glUniformMatrix4fv(textureTransformReference,1,false,textureTransform.getArray(),0);
        glUniformMatrix4fv(mvpMatrixReference,1,false,mvpMatrixBuffer);//uniform mvp matrix
        glUniform1i(texDataReference,0);

        glDrawElements(GL_TRIANGLES,indices.length,GL_UNSIGNED_INT,0);

        glDisableVertexAttribArray(positionReference);
        glDisableVertexAttribArray(texCoordinateReference);
    }
}
