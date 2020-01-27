package com.video.application;

import android.app.Activity;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.os.Environment;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.video.videolib.LogUtils;
import com.video.videolib.VideoProcessorUtils;
import com.video.videolib.callback.IVideoProcessorListener;

import java.io.File;

public class MainActivity extends Activity implements View.OnClickListener {

    private static final String WRITE_EXTERNAL_STORAGE = "android.permission.WRITE_EXTERNAL_STORAGE";
    private static final int REQUEST_PERMISSION_OK = 0x1;

    private static final String ROOT_PATH = Environment.getExternalStorageDirectory() +
            File.separator + "videotest" + File.separator;
    private static final String INPUT_FILE_PATH = ROOT_PATH + "test1.mp4";
    private static final String OUPUT_FILE_PATH = ROOT_PATH + "test1_cut2.mp4";
    private static final String OUPUT_FILE_AUDIO_PATH = ROOT_PATH + "test1_clip_audio.aac";
    private static final String VIDEO_FILE_PATH = ROOT_PATH + "test1_video.mp4";
    private static final String AUDIO_FILE_PATH = ROOT_PATH + "test1_audio.aac";
    private static final String MERGE_OUTPUT_FILE_PATH = ROOT_PATH + "test1_merge_output.mp4";

    private static final String INPUT_FILE1_PATH = ROOT_PATH + "test1.mp4";
    private static final String INPUT_FILE2_PATH = ROOT_PATH + "test2.mp4";
    private static final String OUTPUT_APPEND_FILE_PATH = ROOT_PATH + "append.mp4";

    private static final String INPUT_REVERSE_FILE_PATH = ROOT_PATH + "test1.mp4";
    private static final String OUTPUT_TRANSCODE_FILE_PATH = ROOT_PATH + "transcode_video.mp4";
    private static final String OUTPUT_REVERSE_FILE_PATH = ROOT_PATH + "reverse_video.mp4";

    private TextView mMediaPrintView;
    private TextView mVideoSplitView;
    private TextView mVideoClipView;
    private TextView mVideoMergeView;
    private TextView mVideoAppendView;
    private TextView mVideoReverseView;
    private TextView mVideoKeyFrameView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        initViews();
    }

    private void initViews() {
        mMediaPrintView = (TextView) findViewById(R.id.video_print_view);
        mVideoSplitView = (TextView) findViewById(R.id.video_split_view);
        mVideoClipView = (TextView) findViewById(R.id.video_clip_view);
        mVideoMergeView = (TextView) findViewById(R.id.video_merge_view);
        mVideoAppendView = (TextView) findViewById(R.id.video_append_view);
        mVideoReverseView = (TextView) findViewById(R.id.video_reverse_view);
        mVideoKeyFrameView = (TextView) findViewById(R.id.video_keyframe_view);

        mMediaPrintView.setOnClickListener(this);
        mVideoSplitView.setOnClickListener(this);
        mVideoClipView.setOnClickListener(this);
        mVideoMergeView.setOnClickListener(this);
        mVideoAppendView.setOnClickListener(this);
        mVideoReverseView.setOnClickListener(this);
        mVideoKeyFrameView.setOnClickListener(this);
    }

    @Override
    protected void onResume() {
        super.onResume();
        checkPermission();
    }

    private void checkPermission() {
        if (ContextCompat.checkSelfPermission(this, WRITE_EXTERNAL_STORAGE)
                != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[] { WRITE_EXTERNAL_STORAGE }, REQUEST_PERMISSION_OK);
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        if (requestCode == REQUEST_PERMISSION_OK) {
            if (grantResults.length > 1 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                Toast.makeText(this,  "存储权限已开通", Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(this,  "存储权限被拒绝", Toast.LENGTH_SHORT).show();
            }
        }
    }

    @Override
    public void onClick(View view) {
        if (view == mMediaPrintView) {
            new Thread(new Runnable() {
                @Override
                public void run() {
                    try {
                        VideoProcessorUtils.printMediaInfo(OUTPUT_TRANSCODE_FILE_PATH);
                    } catch (Exception e) {
                        e.printStackTrace();
                        LogUtils.e("printMediaInfo failed, exception = " + e);
                    }
                }
            }).start();
        }
        else if (view == mVideoSplitView) {
            new Thread(new Runnable() {
                @Override
                public void run() {
                    try {
                        VideoProcessorUtils.splitAudioFile(INPUT_FILE_PATH, AUDIO_FILE_PATH);
                    } catch (Exception e) {
                        LogUtils.e("splitAudioFile failed, exception = " + e);
                    }
                }
            }).start();
        } else if (view == mVideoClipView) {
            new Thread(new Runnable() {
                @Override
                public void run() {
                    try {
                        VideoProcessorUtils.cutMedia(INPUT_FILE_PATH, OUPUT_FILE_PATH, 10 * 1000 * 1000, 20 * 1000 * 1000);
                    } catch (Exception e) {
                        LogUtils.e("cutMedia failed, exception = " + e);
                    }
                }
            }).start();
        } else if (view == mVideoMergeView) {
            new Thread(new Runnable() {
                @Override
                public void run() {
                    try {
                        VideoProcessorUtils.mergeMedia(AUDIO_FILE_PATH, VIDEO_FILE_PATH, MERGE_OUTPUT_FILE_PATH);
                    } catch (Exception e) {
                        LogUtils.e("mergeMedia failed, exception = " + e);
                    }
                }
            }).start();
        } else if (view == mVideoAppendView) {
            new Thread(new Runnable() {
                @Override
                public void run() {
                    try {
                        VideoProcessorUtils.appendVideo(INPUT_FILE1_PATH, INPUT_FILE2_PATH, OUTPUT_APPEND_FILE_PATH);
                    } catch (Exception e) {
                        LogUtils.e("appendVideo failed, exception = " + e);
                    }
                }
            }).start();
        } else if (view == mVideoReverseView) {
            new Thread(new Runnable() {
                @Override
                public void run() {
                    try {
                        VideoProcessorUtils.transcodeVideo(INPUT_REVERSE_FILE_PATH, OUTPUT_TRANSCODE_FILE_PATH, new IVideoProcessorListener() {
                            @Override
                            public void onProcessFinished(String outputPath) {
                                try {
                                    LogUtils.d("reverseVideo inputPath=" +outputPath);
                                    VideoProcessorUtils.reverseVideo(outputPath, OUTPUT_REVERSE_FILE_PATH);
                                } catch (Exception e) {
                                    e.printStackTrace();
                                    LogUtils.e("reverseVideo failed, exception = " + e.getMessage());
                                }
                            }
                        });
                    } catch (Exception e) {
                        LogUtils.e("transcodeVideo failed, exception = " + e.getMessage());
                        e.printStackTrace();
                    }
                }
            }).start();
        } else if (view == mVideoKeyFrameView) {
            new Thread(new Runnable() {
                @Override
                public void run() {
                    try {
                        VideoProcessorUtils.getKeyFrames(INPUT_FILE_PATH);
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
            }).start();
        }
    }
}
