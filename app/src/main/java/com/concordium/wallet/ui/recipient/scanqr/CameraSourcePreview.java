/*
 * Copyright (C) The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.concordium.wallet.ui.recipient.scanqr;

import android.Manifest;
import android.content.Context;
import android.content.res.Configuration;
import android.util.AttributeSet;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.ViewGroup;

import androidx.annotation.RequiresPermission;

import com.concordium.wallet.util.Log;
import com.google.android.gms.common.images.Size;

import java.io.IOException;


public class CameraSourcePreview extends ViewGroup {
    private static final String TAG = "CameraSourcePreview";

    private final Context mContext;
    private final SurfaceView mSurfaceView;
    private boolean mStartRequested;
    private boolean mSurfaceAvailable;
    private CameraSource mCameraSource;

    public CameraSourcePreview(Context context, AttributeSet attrs) {
        super(context, attrs);
        mContext = context;
        mStartRequested = false;
        mSurfaceAvailable = false;

        mSurfaceView = new SurfaceView(context);
        mSurfaceView.getHolder().addCallback(new SurfaceCallback());
        addView(mSurfaceView);
    }

    @RequiresPermission(Manifest.permission.CAMERA)
    public void start(CameraSource cameraSource) throws IOException, SecurityException {
        if (cameraSource == null) {
            stop();
        }

        mCameraSource = cameraSource;

        if (mCameraSource != null) {
            mStartRequested = true;
            startIfReady();
        }
    }


    public void stop() {
        if (mCameraSource != null) {
            mCameraSource.stop();
        }
    }

    public void release() {
        if (mCameraSource != null) {
            mCameraSource.release();
            mCameraSource = null;
        }
    }

    @RequiresPermission(Manifest.permission.CAMERA)
    private void startIfReady() throws IOException, SecurityException {
        if (mStartRequested && mSurfaceAvailable) {
            mCameraSource.start(mSurfaceView.getHolder());
            mStartRequested = false;
        }
    }

    private class SurfaceCallback implements SurfaceHolder.Callback {
        @Override
        public void surfaceCreated(SurfaceHolder surface) {
            mSurfaceAvailable = true;
            try {
                startIfReady();
            } catch (SecurityException se) {
                Log.INSTANCE.e("Do not have permission to start the camera", se);
            } catch (IOException e) {
                Log.INSTANCE.e("Could not start camera source.", e);
            }
        }

        @Override
        public void surfaceDestroyed(SurfaceHolder surface) {
            mSurfaceAvailable = false;
        }

        @Override
        public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {
        }
    }

    @Override
    protected void onLayout(boolean changed, int left, int top, int right, int bottom) {
        int width = 320;
        int height = 240;
        if (mCameraSource != null) {
            Size size = mCameraSource.getPreviewSize();
            if (size != null) {
                width = size.getWidth();
                height = size.getHeight();
            }
        }

        // Swap width and height sizes when in portrait, since it will be rotated 90 degrees
        if (isPortraitMode()) {
            int tmp = width;
            //noinspection SuspiciousNameCombination
            width = height;
            height = tmp;
        }

        final int layoutWidth = right - left;
        final int layoutHeight = bottom - top;

        //Fill layout with preview, and let the preview overflow the layout if needed

        // Computes height and width for potentially doing fit width.
        int childWidth = layoutWidth;
        int childHeight = (int) (((float) layoutWidth / (float) width) * height);
        int topOffset = (childHeight - layoutHeight) / 2;
        int leftOffset = 0;

        // If height is not tall enough using fit width, does fit height instead.
        if (childHeight < layoutHeight) {
            childHeight = layoutHeight;
            childWidth = (int) (((float) layoutHeight / (float) height) * width);
            topOffset = 0;
            leftOffset = (childWidth - layoutWidth) / 2;
        }

        for (int i = 0; i < getChildCount(); ++i) {
            getChildAt(i).layout(0 - leftOffset, 0 - topOffset, childWidth - leftOffset, childHeight - topOffset);
        }

        /*
        // Computes height and width for potentially doing fit width.
        int childWidth = layoutWidth;
        int childHeight = (int)(((float) layoutWidth / (float) width) * height);
        int leftOffset = 0;

        // If height is too tall using fit width, does fit height instead.
        if (childHeight > layoutHeight) {
            childHeight = layoutHeight;
            childWidth = (int)(((float) layoutHeight / (float) height) * width);
            leftOffset =  layoutWidth/2 - childWidth/2;
        }

        for (int i = 0; i < getChildCount(); ++i) {
            getChildAt(i).layout(0+leftOffset, 0, childWidth+leftOffset, childHeight);
        }
*/

        try {
            startIfReady();
        } catch (SecurityException se) {
            Log.INSTANCE.e("Do not have permission to start the camera", se);
        } catch (IOException e) {
            Log.INSTANCE.e("Could not start camera source.", e);
        }
    }

    private boolean isPortraitMode() {
        int orientation = mContext.getResources().getConfiguration().orientation;
        if (orientation == Configuration.ORIENTATION_LANDSCAPE) {
            return false;
        }
        if (orientation == Configuration.ORIENTATION_PORTRAIT) {
            return true;
        }

        Log.INSTANCE.d("isPortraitMode returning false by default");
        return false;
    }
}
