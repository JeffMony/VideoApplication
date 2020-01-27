package com.video.videolib;

import android.graphics.Bitmap;
import android.media.MediaCodec;
import android.media.MediaExtractor;
import android.media.MediaFormat;
import android.media.MediaMetadataRetriever;
import android.media.MediaMuxer;

import com.video.videolib.callback.IVideoProcessorListener;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;

public class VideoProcessorUtils {

    //从视频中分离视频流
    public static boolean splitVideoFile(String inputPath, String videoPath) throws IOException {
        MediaMuxer mediaMuxer = null;

        MediaExtractor mediaExtractor = new MediaExtractor();
        mediaExtractor.setDataSource(inputPath);

        int videoTrackIndex = -1;
        for (int index = 0; index < mediaExtractor.getTrackCount(); index++) {
            MediaFormat format = mediaExtractor.getTrackFormat(index);
            LogUtils.w("format = " + format);
            String mime = format.getString(MediaFormat.KEY_MIME);
            if (!mime.startsWith("video/")) {
                continue;
            }
            mediaExtractor.selectTrack(index);

            mediaMuxer = new MediaMuxer(videoPath, MediaMuxer.OutputFormat.MUXER_OUTPUT_MPEG_4);
            videoTrackIndex = mediaMuxer.addTrack(format);
            mediaMuxer.start();
        }

        if (mediaMuxer == null) {
            return false;
        }
        MediaCodec.BufferInfo info = new MediaCodec.BufferInfo();
        info.presentationTimeUs = 0;
        ByteBuffer buffer = ByteBuffer.allocate(500 * 1024);
        int sampleSize = 0;
        while ((sampleSize = mediaExtractor.readSampleData(buffer, 0)) > 0) {
            info.offset = 0;
            info.size = sampleSize;
            info.flags = mediaExtractor.getSampleFlags();
            info.presentationTimeUs = mediaExtractor.getSampleTime();
            mediaMuxer.writeSampleData(videoTrackIndex, buffer, info);
            mediaExtractor.advance();
        }

        mediaExtractor.release();
        mediaMuxer.stop();
        mediaMuxer.release();

        return true;
    }

    //从视频中分离音频
    public static boolean splitAudioFile(String inputPath, String audioPath) throws IOException {
        MediaMuxer mediaMuxer = null;

        MediaExtractor mediaExtractor = new MediaExtractor();
        mediaExtractor.setDataSource(inputPath);

        int audioTrackIndex = -1;
        for (int index = 0; index < mediaExtractor.getTrackCount(); index++) {
            MediaFormat format = mediaExtractor.getTrackFormat(index);
            LogUtils.w("format = " + format);
            String mime = format.getString(MediaFormat.KEY_MIME);
            if (!mime.startsWith("audio/")) {
                continue;
            }
            mediaExtractor.selectTrack(index);

            mediaMuxer = new MediaMuxer(audioPath, MediaMuxer.OutputFormat.MUXER_OUTPUT_MPEG_4);
            audioTrackIndex = mediaMuxer.addTrack(format);
            mediaMuxer.start();
        }

        if (mediaMuxer == null) {
            return false;
        }
        MediaCodec.BufferInfo info = new MediaCodec.BufferInfo();
        info.presentationTimeUs = 0;
        ByteBuffer buffer = ByteBuffer.allocate(500 * 1024);
        int sampleSize = 0;
        while ((sampleSize = mediaExtractor.readSampleData(buffer, 0)) > 0) {
            info.offset = 0;
            info.size = sampleSize;
            info.flags = mediaExtractor.getSampleFlags();
            info.presentationTimeUs = mediaExtractor.getSampleTime();
            mediaMuxer.writeSampleData(audioTrackIndex, buffer, info);
            mediaExtractor.advance();
        }

        mediaExtractor.release();
        mediaMuxer.stop();
        mediaMuxer.release();

        return true;
    }

    //只剪切视频,不剪切音频
    public static boolean cutVideo(String inputPath, String outputPath, long start, long duration) throws IOException {
        MediaMuxer mediaMuxer = null;

        MediaExtractor mediaExtractor = new MediaExtractor();
        mediaExtractor.setDataSource(inputPath);

        int videoTrackIndex = -1;
        for (int index = 0; index < mediaExtractor.getTrackCount(); index++) {
            MediaFormat format = mediaExtractor.getTrackFormat(index);
            LogUtils.w("format = " + format);
            String mime = format.getString(MediaFormat.KEY_MIME);
            if (!mime.startsWith("video/")) {
                continue;
            }
            mediaExtractor.selectTrack(index);

            mediaMuxer = new MediaMuxer(outputPath, MediaMuxer.OutputFormat.MUXER_OUTPUT_MPEG_4);
            videoTrackIndex = mediaMuxer.addTrack(format);
            mediaMuxer.start();
        }

        if (mediaMuxer == null) {
            return false;
        }
        MediaCodec.BufferInfo info = new MediaCodec.BufferInfo();
        info.presentationTimeUs = 0;
        ByteBuffer buffer = ByteBuffer.allocate(500 * 1024);
        int sampleSize = 0;
        while ((sampleSize = mediaExtractor.readSampleData(buffer, 0)) > 0) {
            info.offset = 0;
            info.size = sampleSize;
            info.flags = mediaExtractor.getSampleFlags();
            info.presentationTimeUs = mediaExtractor.getSampleTime();
            if (info.presentationTimeUs <= start) {
                mediaExtractor.advance();
                continue;
            }
            mediaMuxer.writeSampleData(videoTrackIndex, buffer, info);
            if (info.presentationTimeUs > start + duration) {
                break;
            }
            mediaExtractor.advance();
        }

        mediaExtractor.release();
        mediaMuxer.stop();
        mediaMuxer.release();

        return true;

    }

    //剪切视频,也剪切音频
    public static boolean cutMedia(String inputPath, String outputPath, long start, long duration) throws IOException {
        MediaMuxer mediaMuxer = null;

        MediaExtractor mediaExtractor = new MediaExtractor();
        mediaExtractor.setDataSource(inputPath);

        mediaMuxer = new MediaMuxer(outputPath, MediaMuxer.OutputFormat.MUXER_OUTPUT_MPEG_4);

        int sourceVideoTrack = -1;
        int sourceAudioTrack = -1;
        int videoTrackIndex = -1;
        int audioTrackIndex = -1;

        for (int index = 0; index < mediaExtractor.getTrackCount(); index++) {
            MediaFormat format = mediaExtractor.getTrackFormat(index);
            LogUtils.w("format = " + format);
            String mime = format.getString(MediaFormat.KEY_MIME);
            if (mime.startsWith("audio/")) {
                sourceAudioTrack = index;
                audioTrackIndex = mediaMuxer.addTrack(format);
            } else if (mime.startsWith("video/")) {
                sourceVideoTrack = index;
                videoTrackIndex = mediaMuxer.addTrack(format);
            }
        }

        if (mediaMuxer == null) {
            return false;
        }

        mediaMuxer.start();


        //1.cut video track info.
        mediaExtractor.selectTrack(sourceVideoTrack);
        mediaExtractor.seekTo(start, MediaExtractor.SEEK_TO_PREVIOUS_SYNC);
        MediaCodec.BufferInfo info = new MediaCodec.BufferInfo();
        info.presentationTimeUs = 0;
        ByteBuffer buffer = ByteBuffer.allocate(500 * 1024);
        int sampleSize = 0;
        while ((sampleSize = mediaExtractor.readSampleData(buffer, 0)) > 0) {
            info.offset = 0;
            info.size = sampleSize;
            info.flags = mediaExtractor.getSampleFlags();
            info.presentationTimeUs = mediaExtractor.getSampleTime();
//            if (info.presentationTimeUs <= start) {
//                mediaExtractor.advance();
//                continue;
//            }
            mediaMuxer.writeSampleData(videoTrackIndex, buffer, info);
            if (info.presentationTimeUs > start + duration) {
                break;
            }
            mediaExtractor.advance();
        }

        //2.cut audio track info.
        mediaExtractor.unselectTrack(sourceVideoTrack);
        mediaExtractor.selectTrack(sourceAudioTrack);
        mediaExtractor.seekTo(start, MediaExtractor.SEEK_TO_PREVIOUS_SYNC);
        info = new MediaCodec.BufferInfo();
        info.presentationTimeUs = 0;
        buffer = ByteBuffer.allocate(500 * 1024);
        sampleSize = 0;
        while ((sampleSize = mediaExtractor.readSampleData(buffer, 0)) > 0) {
            info.offset = 0;
            info.size = sampleSize;
            info.flags = mediaExtractor.getSampleFlags();
            info.presentationTimeUs = mediaExtractor.getSampleTime();
//            if (info.presentationTimeUs <= start) {
//                mediaExtractor.advance();
//                continue;
//            }
            mediaMuxer.writeSampleData(audioTrackIndex, buffer, info);
            if (info.presentationTimeUs > start + duration) {
                break;
            }
            mediaExtractor.advance();
        }

        mediaExtractor.release();
        mediaMuxer.stop();
        mediaMuxer.release();

        return true;

    }

    //视频音频合并
    public static boolean mergeMedia(String audioPath, String videoPath, String outputPath) throws IOException {
        MediaMuxer mediaMuxer = null;
        mediaMuxer = new MediaMuxer(outputPath, MediaMuxer.OutputFormat.MUXER_OUTPUT_MPEG_4);

        MediaExtractor videoExtractor = new MediaExtractor();
        videoExtractor.setDataSource(videoPath);

        MediaExtractor audioExtractor = new MediaExtractor();
        audioExtractor.setDataSource(audioPath);

        int sourceVideoTrack = -1;
        int videoTrackIndex = -1;
        for (int index = 0; index < videoExtractor.getTrackCount(); index++) {
            MediaFormat format = videoExtractor.getTrackFormat(index);
            LogUtils.w("format = " + format);
            String mime = format.getString(MediaFormat.KEY_MIME);
            if (mime.startsWith("video/")) {
                sourceVideoTrack = index;
                videoTrackIndex = mediaMuxer.addTrack(format);
                break;
            }
        }

        int sourceAudioTrack = -1;
        int audioTrackIndex = -1;
        for (int index = 0; index < audioExtractor.getTrackCount(); index++) {
            MediaFormat format = audioExtractor.getTrackFormat(index);
            LogUtils.w("format = " + format);
            String mime = format.getString(MediaFormat.KEY_MIME);
            if (mime.startsWith("audio/")) {
                sourceAudioTrack = index;
                audioTrackIndex = mediaMuxer.addTrack(format);
                break;
            }
        }

        if (mediaMuxer == null)
            return false;

        mediaMuxer.start();

        //1.write video track info into muxer.
        videoExtractor.selectTrack(sourceVideoTrack);
        MediaCodec.BufferInfo info = new MediaCodec.BufferInfo();
        info.presentationTimeUs = 0;
        ByteBuffer buffer = ByteBuffer.allocate(500 * 1024);
        int sampleSize = 0;
        while ((sampleSize = videoExtractor.readSampleData(buffer, 0)) > 0) {
            info.offset = 0;
            info.size = sampleSize;
            info.flags = videoExtractor.getSampleFlags();
            info.presentationTimeUs = videoExtractor.getSampleTime();
            mediaMuxer.writeSampleData(videoTrackIndex, buffer, info);
            videoExtractor.advance();
        }

        //2.write audio track info into muxer;
        audioExtractor.selectTrack(sourceAudioTrack);
        info = new MediaCodec.BufferInfo();
        info.presentationTimeUs = 0;
        buffer = ByteBuffer.allocate(500 * 1024);
        sampleSize = 0;
        while ((sampleSize = audioExtractor.readSampleData(buffer, 0)) > 0) {
            info.offset = 0;
            info.size = sampleSize;
            info.flags = audioExtractor.getSampleFlags();
            info.presentationTimeUs = audioExtractor.getSampleTime();
            mediaMuxer.writeSampleData(audioTrackIndex, buffer, info);
            audioExtractor.advance();
        }

        videoExtractor.release();
        audioExtractor.release();
        mediaMuxer.stop();
        mediaMuxer.release();

        return true;
    }

    //视频拼接
    public static boolean appendVideo(String inputPath1, String inputPath2, String outputPath) throws IOException {
        MediaMuxer mediaMuxer = null;
        mediaMuxer = new MediaMuxer(outputPath, MediaMuxer.OutputFormat.MUXER_OUTPUT_MPEG_4);

        MediaExtractor videoExtractor1 = new MediaExtractor();
        videoExtractor1.setDataSource(inputPath1);

        MediaExtractor videoExtractor2 = new MediaExtractor();
        videoExtractor2.setDataSource(inputPath2);

        int videoTrackIndex = -1;
        int audioTrackIndex = -1;
        long file1_duration = 0L;

        int sourceVideoTrack1 = -1;
        int sourceAudioTrack1 = -1;
        for (int index = 0; index < videoExtractor1.getTrackCount(); index++) {
            MediaFormat format = videoExtractor1.getTrackFormat(index);
            String mime = format.getString(MediaFormat.KEY_MIME);
            file1_duration = format.getLong(MediaFormat.KEY_DURATION);
            if (mime.startsWith("video/")) {
                sourceVideoTrack1 = index;
                videoTrackIndex = mediaMuxer.addTrack(format);
            } else if (mime.startsWith("audio/")) {
                sourceAudioTrack1 = index;
                audioTrackIndex = mediaMuxer.addTrack(format);
            }
        }

        int sourceVideoTrack2 = -1;
        int sourceAudioTrack2 = -1;
        for (int index = 0; index < videoExtractor2.getTrackCount(); index++) {
            MediaFormat format = videoExtractor2.getTrackFormat(index);
            String mime = format.getString(MediaFormat.KEY_MIME);
            if (mime.startsWith("video/")) {
                sourceVideoTrack2 = index;
            } else if (mime.startsWith("audio/")) {
                sourceAudioTrack2 = index;
            }
        }

        if (mediaMuxer == null)
            return false;

        mediaMuxer.start();
        //1.write first video track into muxer.
        videoExtractor1.selectTrack(sourceVideoTrack1);
        MediaCodec.BufferInfo info = new MediaCodec.BufferInfo();
        info.presentationTimeUs = 0;
        ByteBuffer buffer = ByteBuffer.allocate(500 * 1024);
        int sampleSize = 0;
        while ((sampleSize = videoExtractor1.readSampleData(buffer, 0)) > 0) {
            info.offset = 0;
            info.size = sampleSize;
            info.flags = videoExtractor1.getSampleFlags();
            info.presentationTimeUs = videoExtractor1.getSampleTime();
            mediaMuxer.writeSampleData(videoTrackIndex, buffer, info);
            videoExtractor1.advance();
        }

        //2.write first audio track into muxer.
        videoExtractor1.unselectTrack(sourceVideoTrack1);
        videoExtractor1.selectTrack(sourceAudioTrack1);
        info = new MediaCodec.BufferInfo();
        info.presentationTimeUs = 0;
        buffer = ByteBuffer.allocate(500 * 1024);
        sampleSize = 0;
        while ((sampleSize = videoExtractor1.readSampleData(buffer, 0)) > 0) {
            info.offset = 0;
            info.size = sampleSize;
            info.flags = videoExtractor1.getSampleFlags();
            info.presentationTimeUs = videoExtractor1.getSampleTime();
            mediaMuxer.writeSampleData(audioTrackIndex, buffer, info);
            videoExtractor1.advance();
        }

        //3.write second video track into muxer.
        videoExtractor2.selectTrack(sourceVideoTrack2);
        info = new MediaCodec.BufferInfo();
        info.presentationTimeUs = 0;
        buffer = ByteBuffer.allocate(500 * 1024);
        sampleSize = 0;
        while ((sampleSize = videoExtractor2.readSampleData(buffer, 0)) > 0) {
            info.offset = 0;
            info.size = sampleSize;
            info.flags = videoExtractor2.getSampleFlags();
            info.presentationTimeUs = videoExtractor2.getSampleTime() + file1_duration;
            mediaMuxer.writeSampleData(videoTrackIndex, buffer, info);
            videoExtractor2.advance();
        }

        //4.write second audio track into muxer.
        videoExtractor2.unselectTrack(sourceVideoTrack2);
        videoExtractor2.selectTrack(sourceAudioTrack2);
        info = new MediaCodec.BufferInfo();
        info.presentationTimeUs = 0;
        buffer = ByteBuffer.allocate(500 * 1024);
        sampleSize = 0;
        while ((sampleSize = videoExtractor2.readSampleData(buffer, 0)) > 0) {
            info.offset = 0;
            info.size = sampleSize;
            info.flags = videoExtractor2.getSampleFlags();
            info.presentationTimeUs = videoExtractor2.getSampleTime() + file1_duration;
            mediaMuxer.writeSampleData(audioTrackIndex, buffer, info);
            videoExtractor2.advance();
        }

        videoExtractor1.release();
        videoExtractor2.release();
        mediaMuxer.stop();
        mediaMuxer.release();

        return true;
    }

    public static boolean transcodeVideo(String inputPath, String outputPath, IVideoProcessorListener listener) throws IOException {
        MediaMuxer mediaMuxer = null;
        mediaMuxer = new MediaMuxer(outputPath, MediaMuxer.OutputFormat.MUXER_OUTPUT_MPEG_4);

        MediaExtractor mediaExtractor = new MediaExtractor();
        mediaExtractor.setDataSource(inputPath);

        int videoTrackIndex = -1;
        int sourceVideoTrack = -1;
        long duration = 0L;
        for(int index = 0; index < mediaExtractor.getTrackCount(); index++) {
            MediaFormat format = mediaExtractor.getTrackFormat(index);
            String mime = format.getString(MediaFormat.KEY_MIME);
            duration = format.getLong(MediaFormat.KEY_DURATION);
            if (mime.startsWith("video/")) {
                sourceVideoTrack = index;
                videoTrackIndex = mediaMuxer.addTrack(format);
                break;
            }
        }

        if (mediaMuxer == null)
            return false;

        mediaMuxer.start();

        mediaExtractor.selectTrack(sourceVideoTrack);
        ByteBuffer buffer = ByteBuffer.allocate(500 * 1024);
        MediaCodec.BufferInfo info = new MediaCodec.BufferInfo();
        info.presentationTimeUs = 0;
        int sampleSize = 0;
        while ((sampleSize = mediaExtractor.readSampleData(buffer, 0)) > 0) {
            int flags = mediaExtractor.getSampleFlags();

            if (flags > 0 && (flags & MediaExtractor.SAMPLE_FLAG_SYNC) != 0) {

            } else {
                flags |= MediaExtractor.SAMPLE_FLAG_SYNC;
            }
            info.offset = 0;
            info.flags = flags;
            info.size = sampleSize;
            info.presentationTimeUs = mediaExtractor.getSampleTime();
            mediaMuxer.writeSampleData(videoTrackIndex, buffer, info);
            mediaExtractor.advance();
        }

        mediaExtractor.release();
        mediaMuxer.stop();
        mediaMuxer.release();
        listener.onProcessFinished(outputPath);
        return true;
    }

    public static boolean reverseVideo(String inputPath, String outputPath) throws IOException {
        MediaMuxer mediaMuxer = new MediaMuxer(outputPath, MediaMuxer.OutputFormat.MUXER_OUTPUT_MPEG_4);
        MediaExtractor videoExtractor = new MediaExtractor();
        videoExtractor.setDataSource(inputPath);

        int sourceVideoTrack = -1;
        int videoTrackIndex = -1;
        for(int index = 0; index < videoExtractor.getTrackCount(); index++) {
            MediaFormat format = videoExtractor.getTrackFormat(index);
            String mime = format.getString(MediaFormat.KEY_MIME);
            if (mime.startsWith("video/")) {
                sourceVideoTrack = index;
                videoTrackIndex = mediaMuxer.addTrack(format);
                break;
            }
        }
        if (mediaMuxer == null)
            return false;

        mediaMuxer.start();

        List<Long> frameTimeList = new ArrayList<>();
        videoExtractor.selectTrack(sourceVideoTrack);
        ByteBuffer buffer = ByteBuffer.allocate(500 * 1024);
        int sampleSize = 0;
        while((sampleSize = videoExtractor.readSampleData(buffer, 0)) > 0) {
            long sampleTime = videoExtractor.getSampleTime();
            frameTimeList.add(sampleTime);
        }

        videoExtractor.release();
        mediaMuxer.stop();
        mediaMuxer.release();

        return true;
    }

    public static boolean printMediaInfo(String inputPath) throws IOException {
        MediaExtractor mediaExtractor = new MediaExtractor();
        mediaExtractor.setDataSource(inputPath);

        for(int index = 0; index < mediaExtractor.getTrackCount(); index++) {
            MediaFormat format = mediaExtractor.getTrackFormat(index);
            LogUtils.d(format.toString());
        }

        mediaExtractor.release();
        return true;
    }

    public static boolean getKeyFrames(String inputPath) throws IOException {
        MediaMetadataRetriever mRetriever = new MediaMetadataRetriever();
        mRetriever.setDataSource(inputPath);

        MediaExtractor mediaExtractor = new MediaExtractor();
        mediaExtractor.setDataSource(inputPath);

        int sourceVideoTrack = -1;
        for (int index=0; index < mediaExtractor.getTrackCount(); index++) {
            MediaFormat format = mediaExtractor.getTrackFormat(index);
            String mime = format.getString(MediaFormat.KEY_MIME);
            if (mime.startsWith("video/")) {
                sourceVideoTrack = index;
                break;
            }
        }

        if (sourceVideoTrack == -1)
            return false;

        mediaExtractor.selectTrack(sourceVideoTrack);
        ByteBuffer buffer = ByteBuffer.allocate(500 * 1024);
        List<Long> frameTimeList = new ArrayList<>();
        int sampleSize = 0;
        while((sampleSize = mediaExtractor.readSampleData(buffer, 0)) > 0) {
            int flags = mediaExtractor.getSampleFlags();
            if (flags > 0 && (flags & MediaExtractor.SAMPLE_FLAG_SYNC) != 0) {
                frameTimeList.add(mediaExtractor.getSampleTime());
            }
            mediaExtractor.advance();
        }
        LogUtils.d("getKeyFrames keyFrameCount = " + frameTimeList.size());

        String parentPath = (new File(inputPath)).getParent() + File.separator;
        LogUtils.d("getKeyFrames parent Path="+parentPath);
        for(int index = 0; index < frameTimeList.size(); index++) {
            Bitmap bitmap = mRetriever.getFrameAtTime(frameTimeList.get(index), MediaMetadataRetriever.OPTION_CLOSEST_SYNC);
            savePicFile(bitmap, parentPath + "test_pic_" + index + ".jpg");

        }
        mediaExtractor.release();
        mRetriever.release();
        return true;
    }

    private static void savePicFile(Bitmap bitmap, String savePath) throws IOException {
        if (bitmap == null) {
            LogUtils.d("savePicFile failed, bitmap is null.");
            return;
        }
        LogUtils.d("savePicFile step 1, bitmap is not null.");
        File file = new File(savePath);
        if (!file.exists()) {
            file.createNewFile();
        }
        FileOutputStream outputStream = new FileOutputStream(file);
        bitmap.compress(Bitmap.CompressFormat.JPEG, 100, outputStream);
        outputStream.flush();
        outputStream.close();
    }
}
