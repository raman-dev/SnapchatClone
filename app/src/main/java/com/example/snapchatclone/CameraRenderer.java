package com.example.snapchatclone;

import android.content.Context;
import android.graphics.SurfaceTexture;
import android.opengl.GLSurfaceView;
import android.util.Size;
import android.view.Surface;

import javax.microedition.khronos.egl.EGLConfig;
import javax.microedition.khronos.opengles.GL10;

import graphics.CameraQuad;
import graphics.RenderObject;

import static android.opengl.GLES11Ext.GL_TEXTURE_EXTERNAL_OES;
import static android.opengl.GLES20.GL_COLOR_BUFFER_BIT;
import static android.opengl.GLES20.glClear;
import static android.opengl.GLES20.glClearColor;
import static android.opengl.GLES20.glFlush;
import static android.opengl.GLES20.glViewport;

class CameraRenderer implements GLSurfaceView.Renderer, SurfaceTexture.OnFrameAvailableListener {

    private CameraOperationManager mCameraOperationManager;
    private CameraGLSurfaceView mCameraGLSurfaceView;
    private SurfaceTexture mSurfaceTexture;

    private CameraQuad cameraQuad;

    private float[] viewMatrix;
    private float[] projectionMatrix;
    private float[] mvpMatrix;

    private Context context;
    private int displayRotation = -1;
    private int cameraOrientation = -1;

    public CameraRenderer(Context context, CameraOperationManager mCameraOperationManager, CameraGLSurfaceView mCameraGLSurfaceView) {
        this.mCameraGLSurfaceView = mCameraGLSurfaceView;
        this.mCameraOperationManager = mCameraOperationManager;
        this.context = context;

        viewMatrix = new float[16];
        projectionMatrix = new float[16];
        mvpMatrix = new float[16];
    }

    @Override
    public void onSurfaceCreated(GL10 gl, EGLConfig config) {
        glClearColor(0f,0f,0f,1f);
    }


    @Override
    public void onSurfaceChanged(GL10 gl, int width, int height) {
        //no need to recreate surface texture multiple times
        if(mSurfaceTexture == null) {
            float h = (float) height;
            float w = (float) width;
            //a quad requires 4 vertices in ccw winding order
            //viewport will be w wide and h tall
            //left most point willl be -w/2 and lowest point will be -h/2
            //00 is top left of texture and 11 is bottom right
            //to produce upright images assuming aspect ratio is the same of image
            //for images in the same orientation as the display tex coordinates are 1 to 1
            //for images in the perpendicular orientation the tex coordinates are swapped for top left and bottom right

            float[] verticesAndTexture = null;
            //need to get aspect ratio
            Size outputSize = null;// = new Size(height,width);
            boolean swapDimensions = false;
            switch(cameraOrientation){
                case Surface.ROTATION_0:
                case Surface.ROTATION_180:
                    System.out.println("Camera is 0 | 180 degrees from natural orientation");//meaning camera is in natural orientation
                    //both have the same orientation
                    if(displayRotation != Surface.ROTATION_0 && displayRotation != Surface.ROTATION_180){
                        swapDimensions = true;
                    }
                    break;
                case Surface.ROTATION_90:
                case Surface.ROTATION_270:
                    System.out.println("Camera is 90|270 degrees from natural orientation");
                    //both have same orientation
                    if(displayRotation != Surface.ROTATION_90 && displayRotation != Surface.ROTATION_270){
                        swapDimensions = true;
                    }
                    break;
            }
            if(swapDimensions){
                verticesAndTexture = new float[]{
                       -w/2, h/2,0f,1f, 0f, 1f, //top-left 01
                       -w/2,-h/2,0f,1f, 1f, 1f, //bottom-left 11
                        w/2,-h/2,0f,1f, 1f, 0f, //bottom-right 10
                        w/2, h/2,0f,1f, 0f, 0f, //top-right 00
                };
                outputSize = mCameraOperationManager.getApproximateSize(height,width);//swap since they are perpendicular
            }else{
                verticesAndTexture = new float[]{
                        -w/2, h/2,0f,1f, 0f, 0f,
                        -w/2,-h/2,0f,1f, 0f, 1f,
                        w/2,-h/2,0f,1f, 1f, 1f,
                        w/2, h/2,0f,1f, 1f, 0f,
                };
                outputSize = mCameraOperationManager.getApproximateSize(width,height);//swap since they are perpendicular
            }
            //depending on the orientation of the display to the sensor
            //the texture coordinates need to change
            //if the sensor and display have the same orientation then
            //tex coordinates become 1 to 1 mappings
            //the indices would be 0,1,2,2,3,0
             configureSurfaceAndCameraQuad(verticesAndTexture,outputSize.getWidth(),outputSize.getHeight());
            mCameraOperationManager.addSurface(new Surface(mSurfaceTexture));
            //set mvp matrix here
            //calculate an orthographic projection depending on viewport height and width
            //this is so triangles can be created in a space from -width/2 to +width/2 and -height/2 to + height/2
            RenderObject.computeOrthoMVP(width, height, -10f, 10f, viewMatrix, projectionMatrix, mvpMatrix);
            //set the mvpMatrix for the triangle
            cameraQuad.setMvpMatrix(mvpMatrix);
            //send vertex and index data to gpu and get uniform and attribute references
            cameraQuad.setAttributeAndVBO();
            glViewport(0, 0, width, height);
        }
    }

    private void configureSurfaceAndCameraQuad(float[] verticesAndTexture,int width, int height) {
        cameraQuad = new CameraQuad(verticesAndTexture);
        cameraQuad.createProgram(context, R.raw.vertex_camera, R.raw.fragment_camera);
        cameraQuad.loadTexture(context, -1, GL_TEXTURE_EXTERNAL_OES);

        mSurfaceTexture = new SurfaceTexture(cameraQuad.texDataReference);
        mSurfaceTexture.setDefaultBufferSize(width,height);
        mSurfaceTexture.setOnFrameAvailableListener(this);
    }

    @Override
    public void onDrawFrame(GL10 gl) {
        glClear(GL_COLOR_BUFFER_BIT);
        mSurfaceTexture.updateTexImage();//get the new frame from the camera
        cameraQuad.draw();
        glFlush();
    }

    public void setSensorRotation(int cameraOrientation) {
        this.cameraOrientation = cameraOrientation;
    }

    public void setDisplayRotation(int rotation) {
        this.displayRotation = rotation;
    }

    public void onPause() {
        if(mSurfaceTexture != null){
            mSurfaceTexture.release();
            mSurfaceTexture = null;
        }
    }

    @Override
    public void onFrameAvailable(SurfaceTexture surfaceTexture) {
        //tell the surface view to redraw its contents when a new frame from the camera is available
        mCameraGLSurfaceView.requestRender();
    }
}