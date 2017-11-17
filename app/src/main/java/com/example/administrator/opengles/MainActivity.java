package com.example.administrator.opengles;

import android.opengl.GLES20;
import android.opengl.GLSurfaceView;
import android.opengl.Matrix;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.widget.TextView;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;

import javax.microedition.khronos.egl.EGLConfig;
import javax.microedition.khronos.opengles.GL10;

public class MainActivity extends AppCompatActivity {
    private static final String TAG = "opengles";
    private GLSurfaceView mTriangleSurface;
    // Used to load the 'native-lib' library on application startup.
    static {
        System.loadLibrary("native-lib");
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Example of a call to a native method
        TextView tv = (TextView) findViewById(R.id.sample_text);
        tv.setText(stringFromJNI());
        mTriangleSurface = (GLSurfaceView)findViewById(R.id.triangle_surface);
        mTriangleSurface.setEGLContextClientVersion(2);
        //mTriangleSurface.setEGLConfigChooser(8, 8, 8, 8, 16, 0);
        mTriangleSurface.setRenderer(new TriangleRenderer());
        mTriangleSurface.setRenderMode(GLSurfaceView.RENDERMODE_CONTINUOUSLY);
    }

    private static class TriangleRenderer implements GLSurfaceView.Renderer {
        //gl_Position和gl_FragColor都是Shader的内置变量，分别为定点位置和片元颜色
        private static String vertexShaderCode = "attribute vec4 vPosition;\n"+
                "uniform mat4 vMatrix;\n" +
                "void main() {\n"+
                    "gl_Position = vMatrix*vPosition;\n" +
                "}";
/*
        private static String fragmentShaderCode = "precision mediump float;" +
                "void main() {\n" +
                    "gl_FragColor = vec4(0.5, 0, 0, 1);\n" +
                "}";
*/

        private final String fragmentShaderCode =
                "precision mediump float;" +
                        "uniform vec4 vColor;" +
                        "void main() {" +
                        "  gl_FragColor = vColor;" +
                        "}";

        //渲染范围
        float triangleCoords[] = {
                0.5f,  0.5f, 0.0f, // top
                -0.5f, -0.5f, 0.0f, // bottom left
                0.5f, -0.5f, 0.0f  // bottom right
        };

        //颜色数据
        float color[] = { 1.0f, 1.0f, 1.0f, 1.0f }; //白色
        FloatBuffer vertexBuffer;
        private int mProgram;
        private int mPositionHandle;
        private int mColorHandle;
        static final int COORDS_PER_VERTEX = 3;
        private final int vertexStride = COORDS_PER_VERTEX * 4; // 每个顶点四个字节
        //顶点个数
        private final int vertexCount = triangleCoords.length / COORDS_PER_VERTEX;

        static int loadShader(int type, String shaderCode) {
            int shader = GLES20.glCreateShader(type);
            GLES20.glShaderSource(shader, shaderCode);
            GLES20.glCompileShader(shader);
            return shader;
        }
        private  int mMatrixHandler;
        @Override
        public void onSurfaceCreated(GL10 gl10, EGLConfig eglConfig) {
            Log.d(TAG, "onSurfaceCreated");
            //将背景设置为灰色
            GLES20.glClearColor(0.5f,0.5f,0.5f,1.0f);
            ByteBuffer bb = ByteBuffer.allocateDirect(triangleCoords.length * 4);
            bb.order(ByteOrder.nativeOrder());
            //将坐标数据转换为FloatBuffer，用以传入给OpenGL ES程序
            vertexBuffer = bb.asFloatBuffer();
            vertexBuffer.put(triangleCoords);
            vertexBuffer.position(0);

            int vertexShader = loadShader(GLES20.GL_VERTEX_SHADER,
                    vertexShaderCode);
            int fragmentShader = loadShader(GLES20.GL_FRAGMENT_SHADER,
                    fragmentShaderCode);

            //创建一个空的OpenGLES程序
            mProgram = GLES20.glCreateProgram();
            //将顶点着色器加入到程序
            GLES20.glAttachShader(mProgram, vertexShader);
            //将片元着色器加入到程序中
            GLES20.glAttachShader(mProgram, fragmentShader);
            //连接到着色器程序
            GLES20.glLinkProgram(mProgram);
            //将程序加入到OpenGLES2.0环境
            GLES20.glUseProgram(mProgram);
            //获取变换矩阵vMatrix成员句柄
            mMatrixHandler= GLES20.glGetUniformLocation(mProgram,"vMatrix");
            //指定vMatrix的值
            //GLES20.glUniformMatrix4fv(mMatrixHandler,1,false,mMVPMatrix,0);
            //获取顶点着色器的vPosition成员句柄
            mPositionHandle = GLES20.glGetAttribLocation(mProgram, "vPosition");
            //启用三角形顶点的句柄
            GLES20.glEnableVertexAttribArray(mPositionHandle);
            //准备三角形的坐标数据
            GLES20.glVertexAttribPointer(mPositionHandle, COORDS_PER_VERTEX,
                    GLES20.GL_FLOAT, false,
                    vertexStride, vertexBuffer);
            //获取片元着色器的vColor成员的句柄
            mColorHandle = GLES20.glGetUniformLocation(mProgram, "vColor");
            //设置绘制三角形的颜色
            GLES20.glUniform4fv(mColorHandle, 1, color, 0);
            Log.d("opengles", "handler:"+String.valueOf(mMatrixHandler)+", "+String.valueOf(mPositionHandle)
                    + ", " + String.valueOf(mColorHandle));
        }

        @Override
        public void onDrawFrame(GL10 gl10) {
            Log.d(TAG, "onDrawFrame");
            GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT);//设置背景色

            //绘制三角形
            GLES20.glDrawArrays(GLES20.GL_TRIANGLES, 0, vertexCount);
            //禁止顶点数组的句柄,这里肯定是不能禁止的，除非创建也放到这里来，重复创建他
            //GLES20.glDisableVertexAttribArray(mPositionHandle);
        }
        private float [] mProjectMatrix = new float[16];
        private float [] mViewMatrix = new float[16];
        private float [] mMVPMatrix = new float[16];
        @Override
        public void onSurfaceChanged(GL10 gl10, int i, int i1) {
            GLES20.glViewport(0, 0, i, i1);
            //设置宽高比
            float ratio = (float)i/i1;
            Matrix.frustumM(mProjectMatrix, 0, -ratio, ratio, -1, 1, 3, 7);
            //设置相机位置
            Matrix.setLookAtM(mViewMatrix, 0, 0, 0, 7.0f, 0f, 0f, 0f, 0f, 1.0f, 0.0f);
            //计算变换矩阵
            Matrix.multiplyMM(mMVPMatrix,0,mProjectMatrix,0,mViewMatrix,0);
            //指定vMatrix的值
            GLES20.glUniformMatrix4fv(mMatrixHandler,1,false,mMVPMatrix,0);
            Log.d(TAG, "onSurfaceChanged");
        }
    }
    /**
     * A native method that is implemented by the 'native-lib' native library,
     * which is packaged with this application.
     */
    public native String stringFromJNI();
}
