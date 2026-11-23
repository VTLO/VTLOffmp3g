package com.vtlo.offmp3g;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * One runnable entry of tools/ffmpeg.sh.
 *
 * Step templates use two kinds of placeholders:
 *   {name}  - a user-editable parameter, substituted when the command preview is built
 *   <name>  - a file, resolved only at run time (picked inputs, the output, temp files)
 */
public final class Recipe {

    /**
     * FILE and DIRECTORY write results; NONE streams out (RTMP) and REPORT only measures: its
     * result is ffmpeg's log (media info, loudness, scene cuts…).
     */
    public enum OutputKind { FILE, DIRECTORY, NONE, REPORT }

    /** A text file the job writes before running, e.g. subtitles made from the user's text. */
    public static final class Generated {
        public final String key;
        public final String fileName;
        public final String template;

        Generated(String key, String fileName, String template) {
            this.key = key;
            this.fileName = fileName;
            this.template = template;
        }
    }

    public static final class Input {
        public final String key;
        public final String label;
        public final String mime;
        /** Several files at once (used by the concat recipe). */
        public final boolean multiple;
        /** Copy to a real path instead of streaming via SAF (subtitle filter, concat list). */
        public final boolean copy;

        Input(String key, String label, String mime, boolean multiple, boolean copy) {
            this.key = key;
            this.label = label;
            this.mime = mime;
            this.multiple = multiple;
            this.copy = copy;
        }
    }

    public static final class Param {
        public final String key;
        public final String label;
        public final String defaultValue;

        Param(String key, String label, String defaultValue) {
            this.key = key;
            this.label = label;
            this.defaultValue = defaultValue;
        }
    }

    public final String id;
    public final String section;
    public final String title;
    public final String description;
    /** The line exactly as it appears in the cheatsheet. */
    public final String original;
    public final List<Input> inputs;
    public final List<Param> params;
    public final List<String> steps;
    /** Files written by the job, referenced as {@code <key>} in the steps. */
    public final List<Generated> generated;
    public final OutputKind outputKind;
    /** Output extension, may reference params, e.g. "{format}". */
    public final String outputExt;
    public final String outputMime;

    private Recipe(Builder b) {
        id = b.id;
        section = b.section;
        title = b.title;
        description = b.description;
        original = b.original;
        inputs = Collections.unmodifiableList(b.inputs);
        params = Collections.unmodifiableList(b.params);
        steps = Collections.unmodifiableList(b.steps);
        generated = Collections.unmodifiableList(b.generated);
        outputKind = b.outputKind;
        outputExt = b.outputExt;
        outputMime = b.outputMime;
    }

    public Map<String, String> defaultParams() {
        Map<String, String> m = new LinkedHashMap<>();
        for (Param p : params) m.put(p.key, p.defaultValue);
        return m;
    }

    public static String applyParams(String template, Map<String, String> values) {
        String s = template;
        for (Map.Entry<String, String> e : values.entrySet()) {
            s = s.replace("{" + e.getKey() + "}", e.getValue());
        }
        return s;
    }

    /** The full multi-line command text shown to (and editable by) the user. */
    public String buildCommand(Map<String, String> values) {
        StringBuilder sb = new StringBuilder();
        for (String step : steps) {
            if (sb.length() > 0) sb.append('\n');
            sb.append("ffmpeg ").append(applyParams(step, values));
        }
        return sb.toString();
    }

    /** Contents of the generated files for these parameter values: key → {file name, text}. */
    public Map<String, String[]> generatedFiles(Map<String, String> values) {
        Map<String, String[]> files = new LinkedHashMap<>();
        for (Generated g : generated) {
            files.put(g.key, new String[]{g.fileName, applyParams(g.template, values)});
        }
        return files;
    }

    public Input findInput(String key) {
        for (Input i : inputs) if (i.key.equals(key)) return i;
        return null;
    }

    // ------------------------------------------------------------------------------------------

    private static final class Builder {
        final String id, section, title;
        String description = "", original = "";
        final List<Input> inputs = new ArrayList<>();
        final List<Param> params = new ArrayList<>();
        final List<String> steps = new ArrayList<>();
        final List<Generated> generated = new ArrayList<>();
        OutputKind outputKind = OutputKind.FILE;
        String outputExt = "mp4", outputMime = "video/mp4";

        Builder(String id, String section, String title) {
            this.id = id;
            this.section = section;
            this.title = title;
        }

        Builder desc(String d) { description = d; return this; }
        Builder orig(String o) { original = o; return this; }
        Builder video() { return in("input", "Input video", "video/*"); }
        /** A video or an audio file. */
        Builder media() { return in("input", "Input file (video or audio)", "*/*"); }
        Builder in(String key, String label, String mime) {
            inputs.add(new Input(key, label, mime, false, false)); return this;
        }
        Builder inCopy(String key, String label, String mime) {
            inputs.add(new Input(key, label, mime, false, true)); return this;
        }
        Builder inMulti(String key, String label, String mime) {
            inputs.add(new Input(key, label, mime, true, true)); return this;
        }
        Builder param(String key, String label, String def) {
            params.add(new Param(key, label, def)); return this;
        }
        Builder step(String s) { steps.add(s); return this; }
        Builder out(String ext, String mime) { outputExt = ext; outputMime = mime; return this; }
        Builder outDir() { outputKind = OutputKind.DIRECTORY; outputExt = ""; outputMime = "image/jpeg"; return this; }
        Builder noOut() { outputKind = OutputKind.NONE; outputExt = ""; outputMime = ""; return this; }
        Builder report() { outputKind = OutputKind.REPORT; outputExt = ""; outputMime = ""; return this; }
        Builder gen(String key, String fileName, String template) {
            generated.add(new Generated(key, fileName, template)); return this;
        }
        Recipe build() { return new Recipe(this); }
    }

    public static final String TRANSCODE = "Transcode";
    public static final String COMPRESSION = "Compression & quality";
    public static final String AUDIO = "Audio";
    public static final String GIF = "GIF";
    public static final String CUT = "Cut & join";
    public static final String SCALE = "Scale, crop, overlay";
    public static final String FRAMES = "Frames & streaming";
    public static final String CUSTOM = "Custom";
    public static final String SPEED = "Speed & time";
    public static final String PICTURE = "Picture & colour";
    public static final String TEXT = "Text & overlays";
    public static final String AUDIO_FX = "Audio clean-up & mix";
    public static final String DELIVERY = "Delivery";
    public static final String ANALYSIS = "Analysis";

    public static final List<Recipe> ALL = Arrays.asList(
        // --- TRANSCODE ---------------------------------------------------------------------
        new Builder("to_mp4", TRANSCODE, "Anything → MP4 (H.264 + AAC)")
            .desc("Plays everywhere.")
            .orig("ffmpeg -i input.avi -c:v libx264 -c:a aac output.mp4")
            .video()
            .step("-i <input> -c:v libx264 -c:a aac <output>")
            .build(),
        new Builder("to_webm", TRANSCODE, "MP4 → WebM (VP9 + Opus)")
            .desc("For the web. VP9 software encoding is slow on phones.")
            .orig("ffmpeg -i input.mp4 -c:v libvpx-vp9 -b:v 2M -c:a libopus output.webm")
            .video()
            .param("vbitrate", "Video bitrate", "2M")
            .step("-i <input> -c:v libvpx-vp9 -b:v {vbitrate} -c:a libopus <output>")
            .out("webm", "video/webm")
            .build(),
        new Builder("webm_vpre", TRANSCODE, "MP4 → WebM (VP8, libvpx preset)")
            .desc("Uses FFmpeg's bundled libvpx presets: libvpx-360p, libvpx-720p, libvpx-720p50_60, "
                + "libvpx-1080p, libvpx-1080p50_60. The presets favour quality (cpu-used=0), so encoding is slow.")
            .orig("ffmpeg -i input.mp4 -vpre libvpx-720p -c:a libopus -b:a 128k output.webm")
            .video()
            .param("preset", "Preset", "libvpx-720p")
            // The presets set a global b=..., which libopus rejects unless the audio bitrate is explicit.
            .param("abitrate", "Audio bitrate", "128k")
            .step("-i <input> -vpre {preset} -c:a libopus -b:a {abitrate} <output>")
            .out("webm", "video/webm")
            .build(),
        new Builder("remux", TRANSCODE, "Remux only (change container)")
            .desc("No re-encoding, no quality loss.")
            .orig("ffmpeg -i input.mkv -c copy output.mp4")
            .video()
            .param("format", "Output container (mp4, mkv, mov…)", "mp4")
            .step("-i <input> -c copy <output>")
            .out("{format}", "video/*")
            .build(),

        // --- COMPRESSION -------------------------------------------------------------------
        new Builder("crf", COMPRESSION, "Compress with CRF")
            .desc("CRF 18 = high quality, 28 = small file, 23 = sweet spot.")
            .orig("ffmpeg -i input.mp4 -c:v libx264 -crf 23 -preset medium -c:a aac -b:a 128k output.mp4")
            .video()
            .param("crf", "CRF (0-51)", "23")
            .param("preset", "Preset", "medium")
            .param("abitrate", "Audio bitrate", "128k")
            .step("-i <input> -c:v libx264 -crf {crf} -preset {preset} -c:a aac -b:a {abitrate} <output>")
            .build(),
        new Builder("crf_slow", COMPRESSION, "Compress with slow preset")
            .desc("Slower preset = smaller file at same quality.")
            .orig("ffmpeg -i input.mp4 -c:v libx264 -crf 23 -preset slow output.mp4")
            .video()
            .param("crf", "CRF (0-51)", "23")
            .param("preset", "Preset", "slow")
            .step("-i <input> -c:v libx264 -crf {crf} -preset {preset} <output>")
            .build(),
        new Builder("two_pass", COMPRESSION, "Two-pass encoding (target bitrate)")
            .desc("Pass 1 analyses (no audio, output discarded), pass 2 hits a target size precisely. Both passes run in sequence.")
            .orig("ffmpeg -y -i input.mp4 -c:v libx264 -b:v 1M -pass 1 -an -f null /dev/null\n"
                + "ffmpeg -i input.mp4 -c:v libx264 -b:v 1M -pass 2 -c:a aac -b:a 128k output.mp4")
            .video()
            .param("vbitrate", "Video bitrate", "1M")
            .param("abitrate", "Audio bitrate", "128k")
            .step("-y -i <input> -c:v libx264 -b:v {vbitrate} -pass 1 -passlogfile <passlog> -an -f null /dev/null")
            .step("-i <input> -c:v libx264 -b:v {vbitrate} -pass 2 -passlogfile <passlog> -c:a aac -b:a {abitrate} <output>")
            .build(),

        // --- DELIVERY ----------------------------------------------------------------------
        new Builder("export_youtube", DELIVERY, "Export for YouTube")
            .desc("H.264 up to 1080p, high quality, loudness at -14 LUFS, fast start for streaming.")
            .video()
            .param("crf", "CRF (lower = better)", "18")
            .param("preset", "Preset", "medium")
            .step("-i <input> -vf \"scale=-2:'min(1080,ih)'\" -c:v libx264 -preset {preset} -crf {crf} -pix_fmt yuv420p "
                + "-af loudnorm=I=-14:TP=-1:LRA=11 -c:a aac -b:a 192k -ar 48000 -movflags +faststart <output>")
            .build(),
        new Builder("export_vertical", DELIVERY, "Export for Reels / TikTok / Shorts")
            .desc("Fills a 1080x1920 frame (centre crop), 30 fps, -14 LUFS, cut to the maximum length.")
            .video()
            .param("maxlen", "Maximum length (s)", "90")
            .param("preset", "Preset", "medium")
            .step("-i <input> -t {maxlen} -vf \"scale=1080:1920:force_original_aspect_ratio=increase,crop=1080:1920,setsar=1,fps=30\" "
                + "-c:v libx264 -preset {preset} -crf 20 -pix_fmt yuv420p -af loudnorm=I=-14:TP=-1:LRA=11 "
                + "-c:a aac -b:a 128k -ar 48000 -movflags +faststart <output>")
            .build(),
        new Builder("export_prores", DELIVERY, "ProRes master (editing)")
            .desc("Apple ProRes 422 HQ with PCM audio in a .mov, for editing software. Large files.")
            .video()
            .param("profile", "Profile (0 proxy, 1 LT, 2 standard, 3 HQ)", "3")
            .step("-i <input> -c:v prores_ks -profile:v {profile} -pix_fmt yuv422p10le -c:a pcm_s16le <output>")
            .out("mov", "video/quicktime")
            .build(),
        new Builder("proxy", DELIVERY, "Small proxy / preview file")
            .desc("A light copy to preview or send quickly: reduced width, low bitrate.")
            .video()
            .param("width", "Width", "640")
            .step("-i <input> -vf scale={width}:-2 -c:v libx264 -preset veryfast -crf 30 -c:a aac -b:a 64k <output>")
            .build(),
        new Builder("repair", DELIVERY, "Repair a corrupted recording")
            .desc("Rewrites the container without re-encoding, skipping damaged packets and rebuilding timestamps.")
            .video()
            .param("format", "Output container", "mp4")
            .step("-fflags +genpts+discardcorrupt -err_detect ignore_err -i <input> -map 0 -c copy <output>")
            .out("{format}", "video/*")
            .build(),
        new Builder("image_clip", DELIVERY, "Photo → video clip (Ken Burns)")
            .desc("A slow zoom on a still image, for title cards and slideshows.")
            .in("image", "Image", "image/*")
            .param("duration", "Duration (s)", "6")
            .param("width", "Width", "1920")
            .param("height", "Height", "1080")
            .param("zoom", "Zoom per frame", "0.0015")
            .step("-loop 1 -framerate 30 -i <image> -t {duration} -vf \"scale=8000:-2,zoompan=z='min(1+{zoom}*on,1.5)':d=1:"
                + "x='iw/2-(iw/zoom/2)':y='ih/2-(ih/zoom/2)':s={width}x{height}:fps=30,format=yuv420p\" -c:v libx264 <output>")
            .build(),
        new Builder("color_clip", DELIVERY, "Blank colour clip")
            .desc("A plain colour clip, e.g. a background or a pause between scenes.")
            .param("color", "Colour (name or 0xRRGGBB)", "0x101010")
            .param("duration", "Duration (s)", "3")
            .param("width", "Width", "1920")
            .param("height", "Height", "1080")
            .step("-filter_complex \"color=c={color}:s={width}x{height}:d={duration}:r=30,format=yuv420p\" -c:v libx264 <output>")
            .build(),
        new Builder("grid", DELIVERY, "2×2 video grid")
            .desc("Four videos in a grid; the sound of the first one is kept.")
            .in("input", "Video 1 (top left)", "video/*")
            .in("second", "Video 2 (top right)", "video/*")
            .in("third", "Video 3 (bottom left)", "video/*")
            .in("fourth", "Video 4 (bottom right)", "video/*")
            .param("w", "Cell width", "960")
            .param("h", "Cell height", "540")
            .step("-i <input> -i <second> -i <third> -i <fourth> -filter_complex \""
                + "[0:v]scale={w}:{h},setsar=1[a];[1:v]scale={w}:{h},setsar=1[b];[2:v]scale={w}:{h},setsar=1[c];[3:v]scale={w}:{h},setsar=1[d];"
                + "[a][b][c][d]xstack=inputs=4:layout=0_0|w0_0|0_h0|w0_h0:shortest=1[v]\" -map \"[v]\" -map 0:a? -c:v libx264 -c:a aac <output>")
            .build(),
        new Builder("side_by_side", DELIVERY, "Two videos side by side")
            .desc("Before/after comparisons: both videos at the same height, next to each other.")
            .in("input", "Left video", "video/*")
            .in("second", "Right video", "video/*")
            .param("h", "Height", "720")
            .step("-i <input> -i <second> -filter_complex \"[0:v]scale=-2:{h},setsar=1[a];[1:v]scale=-2:{h},setsar=1[b];"
                + "[a][b]hstack=inputs=2:shortest=1[v]\" -map \"[v]\" -map 0:a? -c:v libx264 -c:a aac <output>")
            .build(),
        new Builder("metadata", DELIVERY, "Set title / artist / comment")
            .desc("Writes the file's tags without re-encoding.")
            .media()
            .param("title", "Title", "My video")
            .param("artist", "Artist", "VTLO")
            .param("comment", "Comment", "Made with VTLOffmp3g")
            .param("format", "Output container", "mp4")
            .step("-i <input> -map 0 -c copy -metadata \"title={title}\" -metadata \"artist={artist}\" -metadata \"comment={comment}\" <output>")
            .out("{format}", "*/*")
            .build(),

        // --- AUDIO -------------------------------------------------------------------------
        new Builder("audio_mp3", AUDIO, "Extract audio as MP3")
            .desc("-vn = no video, -q:a 2 = high VBR quality.")
            .orig("ffmpeg -i input.mp4 -vn -c:a libmp3lame -q:a 2 audio.mp3")
            .video()
            .param("q", "VBR quality (0 best - 9 worst)", "2")
            .step("-i <input> -vn -c:a libmp3lame -q:a {q} <output>")
            .out("mp3", "audio/mpeg")
            .build(),
        new Builder("audio_m4a", AUDIO, "Extract audio as M4A (AAC)")
            .orig("ffmpeg -i input.mp4 -vn -c:a aac -b:a 192k audio.m4a")
            .video()
            .param("abitrate", "Audio bitrate", "192k")
            .step("-i <input> -vn -c:a aac -b:a {abitrate} <output>")
            .out("m4a", "audio/mp4")
            .build(),
        new Builder("audio_copy", AUDIO, "Extract audio without re-encoding")
            .desc("Fastest. The extension must fit the source codec (aac, mp3, opus, ac3…).")
            .orig("ffmpeg -i input.mp4 -vn -c:a copy audio.aac")
            .video()
            .param("format", "Output extension", "aac")
            .step("-i <input> -vn -c:a copy <output>")
            .out("{format}", "audio/*")
            .build(),
        new Builder("replace_audio", AUDIO, "Replace a video's audio track")
            .orig("ffmpeg -i video.mp4 -i new_audio.mp3 -c:v copy -c:a aac -map 0:v:0 -map 1:a:0 -shortest output.mp4")
            .in("input", "Video", "video/*")
            .in("audio", "New audio", "audio/*")
            .step("-i <input> -i <audio> -c:v copy -c:a aac -map 0:v:0 -map 1:a:0 -shortest <output>")
            .build(),

        // --- AUDIO CLEAN-UP & MIX ----------------------------------------------------------
        new Builder("loudnorm", AUDIO_FX, "Normalise loudness (LUFS)")
            .desc("-14 LUFS for streaming platforms, -16 for podcasts, -23 for broadcast. The picture is copied.")
            .media()
            .param("lufs", "Target loudness (LUFS)", "-14")
            .param("tp", "True peak (dBTP)", "-1.5")
            .param("format", "Output container", "mp4")
            .step("-i <input> -c:v copy -af loudnorm=I={lufs}:TP={tp}:LRA=11 -ar 48000 <output>")
            .out("{format}", "*/*")
            .build(),
        new Builder("voice_clean", AUDIO_FX, "Clean up a voice (hiss, rumble)")
            .desc("Cuts low rumble and high hiss, then reduces steady background noise. The picture is copied.")
            .media()
            .param("nr", "Noise reduction (dB)", "12")
            .param("format", "Output container", "mp4")
            .step("-i <input> -c:v copy -af \"highpass=f=80,lowpass=f=12000,afftdn=nr={nr}\" <output>")
            .out("{format}", "*/*")
            .build(),
        new Builder("music_duck", AUDIO_FX, "Music under the voice (ducking)")
            .desc("Adds a music bed that loops for the whole video and drops automatically while someone speaks.")
            .video()
            .in("music", "Music", "audio/*")
            .param("musicvol", "Music volume", "0.35")
            .param("ratio", "Ducking ratio", "8")
            .param("threshold", "Ducking threshold", "0.05")
            .step("-i <input> -stream_loop -1 -i <music> -filter_complex \"[1:a]volume={musicvol}[m];[0:a]asplit=2[voice][sc];"
                + "[m][sc]sidechaincompress=threshold={threshold}:ratio={ratio}:attack=20:release=400[ducked];"
                + "[voice][ducked]amix=inputs=2:duration=first:dropout_transition=0:normalize=0[a]\" "
                + "-map 0:v -map \"[a]\" -c:v copy -c:a aac -b:a 192k <output>")
            .build(),
        new Builder("downmix", AUDIO_FX, "5.1 → stereo")
            .desc("Folds a surround track down to two channels. The picture is copied.")
            .media()
            .param("format", "Output container", "mp4")
            .step("-i <input> -c:v copy -ac 2 <output>")
            .out("{format}", "*/*")
            .build(),
        new Builder("widen", AUDIO_FX, "Wider stereo")
            .desc("Increases the difference between left and right (needs a stereo source).")
            .media()
            .param("amount", "Amount (1 = unchanged)", "1.8")
            .param("format", "Output container", "mp4")
            .step("-i <input> -c:v copy -af extrastereo=m={amount} <output>")
            .out("{format}", "*/*")
            .build(),
        new Builder("mute", AUDIO_FX, "Remove the sound")
            .desc("Keeps the picture as is, without any audio track.")
            .video()
            .step("-i <input> -c:v copy -an <output>")
            .build(),
        new Builder("dynamics", AUDIO_FX, "Compressor + limiter")
            .desc("Evens out a voice and stops the peaks from clipping. The picture is copied.")
            .media()
            .param("threshold", "Compressor threshold", "-20dB")
            .param("ratio", "Ratio", "4")
            .param("limit", "Limiter ceiling (0-1)", "0.89")
            .param("format", "Output container", "mp4")
            .step("-i <input> -c:v copy -af \"acompressor=threshold={threshold}:ratio={ratio}:attack=5:release=80:makeup=2,alimiter=limit={limit}\" <output>")
            .out("{format}", "*/*")
            .build(),
        new Builder("silence_remove", AUDIO_FX, "Remove silences (audio)")
            .desc("Cuts every pause longer than the minimum from an audio track (podcasts, voice notes).")
            .media()
            .param("min", "Minimum pause (s)", "0.6")
            .param("threshold", "Silence level (dB)", "-40")
            .param("format", "Output format", "m4a")
            .step("-i <input> -vn -af \"silenceremove=start_periods=1:start_threshold={threshold}dB:stop_periods=-1:stop_duration={min}:stop_threshold={threshold}dB\" <output>")
            .out("{format}", "audio/*")
            .build(),
        new Builder("audiogram", AUDIO_FX, "Audiogram (podcast → video)")
            .desc("Turns an audio file into a video: your image with a moving waveform at the bottom.")
            .in("audio", "Audio", "audio/*")
            .in("image", "Cover image", "image/*")
            .param("width", "Width", "1080")
            .param("height", "Height", "1080")
            .param("wh", "Waveform height", "240")
            .step("-loop 1 -framerate 30 -i <image> -i <audio> -filter_complex \"[0:v]scale={width}:{height}:force_original_aspect_ratio=increase,"
                + "crop={width}:{height},setsar=1[bg];[1:a]showwaves=s={width}x{wh}:mode=cline:rate=30:colors=0x21E36A[w];"
                + "[bg][w]overlay=0:H-h-40:shortest=1,format=yuv420p[v]\" -map \"[v]\" -map 1:a -c:v libx264 -tune stillimage "
                + "-c:a aac -b:a 192k -shortest <output>")
            .build(),
        new Builder("waveform_video", AUDIO_FX, "Waveform video")
            .desc("An animated waveform of a track on a black background.")
            .in("audio", "Audio", "audio/*")
            .param("size", "Size", "1280x720")
            .step("-i <audio> -filter_complex \"[0:a]showwaves=s={size}:mode=cline:rate=30:colors=0x21E36A,format=yuv420p[v]\" "
                + "-map \"[v]\" -map 0:a -c:v libx264 -c:a aac <output>")
            .build(),
        new Builder("waveform_png", AUDIO_FX, "Waveform image")
            .desc("A picture of the whole track's waveform, e.g. to find the loud parts.")
            .in("audio", "Audio", "audio/*")
            .param("size", "Size", "1920x480")
            .step("-i <audio> -filter_complex \"showwavespic=s={size}:colors=0x21E36A\" -frames:v 1 <output>")
            .out("png", "image/png")
            .build(),

        // --- GIF ---------------------------------------------------------------------------
        new Builder("gif", GIF, "Video → optimized GIF")
            .desc("Step 1 generates an optimized colour palette, step 2 renders the GIF with it.")
            .orig("ffmpeg -i input.mp4 -vf \"fps=10,scale=480:-1:flags=lanczos,palettegen\" palette.png\n"
                + "ffmpeg -i input.mp4 -i palette.png -filter_complex \"fps=10,scale=480:-1:flags=lanczos[x];[x][1:v]paletteuse\" output.gif")
            .video()
            .param("fps", "Frames per second", "10")
            .param("width", "Width (px)", "480")
            .step("-i <input> -vf \"fps={fps},scale={width}:-1:flags=lanczos,palettegen\" <palette>")
            .step("-i <input> -i <palette> -filter_complex \"fps={fps},scale={width}:-1:flags=lanczos[x];[x][1:v]paletteuse\" <output>")
            .out("gif", "image/gif")
            .build(),

        // --- CUT & JOIN --------------------------------------------------------------------
        new Builder("cut_fast", CUT, "Fast cut (stream copy)")
            .desc("-ss before -i seeks fast; cuts land on keyframes.")
            .orig("ffmpeg -ss 00:01:30 -i input.mp4 -t 00:00:30 -c copy clip.mp4")
            .video()
            .param("start", "Start", "00:01:30")
            .param("duration", "Duration", "00:00:30")
            .param("format", "Output container", "mp4")
            .step("-ss {start} -i <input> -t {duration} -c copy <output>")
            .out("{format}", "video/*")
            .build(),
        new Builder("cut_accurate", CUT, "Frame-accurate cut (re-encode)")
            .desc("Re-encodes, slower but exact.")
            .orig("ffmpeg -i input.mp4 -ss 00:01:30 -t 00:00:30 -c:v libx264 -c:a aac clip.mp4")
            .video()
            .param("start", "Start", "00:01:30")
            .param("duration", "Duration", "00:00:30")
            .step("-i <input> -ss {start} -t {duration} -c:v libx264 -c:a aac <output>")
            .build(),
        new Builder("concat", CUT, "Join videos losslessly")
            .desc("Pick the clips in order. They must share the same codecs. The list file is generated for you.")
            .orig("ffmpeg -f concat -safe 0 -i list.txt -c copy joined.mp4")
            .inMulti("videos", "Videos (in order)", "video/*")
            .param("format", "Output container", "mp4")
            .step("-f concat -safe 0 -i <list> -c copy <output>")
            .out("{format}", "video/*")
            .build(),

        new Builder("remove_span", CUT, "Remove a section")
            .desc("Deletes the part between the two times and joins the rest (re-encodes).")
            .video()
            .param("from", "Remove from (s)", "10")
            .param("to", "Remove up to (s)", "20")
            .step("-i <input> -filter_complex \"[0:v]trim=end={from},setpts=PTS-STARTPTS[v1];[0:v]trim=start={to},setpts=PTS-STARTPTS[v2];"
                + "[0:a]atrim=end={from},asetpts=PTS-STARTPTS[a1];[0:a]atrim=start={to},asetpts=PTS-STARTPTS[a2];"
                + "[v1][a1][v2][a2]concat=n=2:v=1:a=1[v][a]\" -map \"[v]\" -map \"[a]\" -c:v libx264 -c:a aac <output>")
            .build(),
        new Builder("xfade_join", CUT, "Join two clips with a transition")
            .desc("Both clips need the same size and frame rate. Start = length of the first clip minus the transition. "
                + "Transitions: fade, wipeleft, slideup, circleopen, dissolve, …")
            .in("input", "First clip", "video/*")
            .in("second", "Second clip", "video/*")
            .param("transition", "Transition", "fade")
            .param("d", "Transition length (s)", "0.5")
            .param("offset", "Transition start (s)", "4.5")
            .step("-i <input> -i <second> -filter_complex \"[0:v][1:v]xfade=transition={transition}:duration={d}:offset={offset},format=yuv420p[v];"
                + "[0:a][1:a]acrossfade=d={d}[a]\" -map \"[v]\" -map \"[a]\" -c:v libx264 -c:a aac <output>")
            .build(),
        new Builder("broll", CUT, "B-roll cutaway")
            .desc("Shows another clip over the main one between two times; the main sound continues.")
            .in("input", "Main video", "video/*")
            .in("broll", "B-roll clip", "video/*")
            .param("at", "From (s)", "2")
            .param("end", "Until (s)", "5")
            .step("-i <input> -i <broll> -filter_complex \"[1:v][0:v]scale2ref[b][base];[b]setpts=PTS-STARTPTS+{at}/TB[bt];"
                + "[base][bt]overlay=enable='between(t,{at},{end})':eof_action=pass[v]\" -map \"[v]\" -map 0:a? -c:v libx264 -c:a copy <output>")
            .build(),

        // --- SPEED & TIME ------------------------------------------------------------------
        new Builder("speed", SPEED, "Change speed (keep pitch)")
            .desc("2 = twice as fast, 0.5 = half speed; voices keep their pitch. Needs a sound track.")
            .video()
            .param("factor", "Speed factor (0.5 to 4)", "2")
            .step("-i <input> -filter_complex \"[0:v]setpts=PTS/{factor}[v];[0:a]atempo={factor}[a]\" -map \"[v]\" -map \"[a]\" "
                + "-c:v libx264 -c:a aac <output>")
            .build(),
        new Builder("slowmo", SPEED, "Smooth slow motion")
            .desc("Slows the picture down and creates in-between frames. mci = smoothest (slow to compute), blend = faster. Sound removed.")
            .video()
            .param("factor", "Slow-down factor", "2")
            .param("fps", "Output frame rate", "60")
            .param("mode", "Interpolation (mci or blend)", "mci")
            .step("-i <input> -vf \"setpts={factor}*PTS,minterpolate=fps={fps}:mi_mode={mode}\" -an -c:v libx264 <output>")
            .build(),
        new Builder("freeze_end", SPEED, "Freeze the last frame")
            .desc("Holds the last picture for a few seconds, with silence.")
            .video()
            .param("hold", "Hold (s)", "2")
            .step("-i <input> -vf tpad=stop_mode=clone:stop_duration={hold} -af apad=pad_dur={hold} -c:v libx264 -c:a aac <output>")
            .build(),
        new Builder("reverse", SPEED, "Play backwards")
            .desc("Reverses picture and sound. The whole clip is held in memory: keep it short.")
            .video()
            .step("-i <input> -vf reverse -af areverse -c:v libx264 -c:a aac <output>")
            .build(),
        new Builder("loop", SPEED, "Loop to fill a duration")
            .desc("Repeats the clip until it reaches the length you choose.")
            .video()
            .param("duration", "Total length (s)", "30")
            .step("-stream_loop -1 -i <input> -t {duration} -c:v libx264 -c:a aac <output>")
            .build(),
        new Builder("pad_start", SPEED, "Add black at the start")
            .desc("Adds black picture and silence before the video starts.")
            .video()
            .param("dur", "Black lead-in (s)", "1.5")
            .step("-i <input> -vf tpad=start_duration={dur}:color=black -af adelay=delays={dur}s:all=1 -c:v libx264 -c:a aac <output>")
            .build(),
        new Builder("fades", SPEED, "Fade in and out")
            .desc("Fades picture and sound in from black at the start and out at the end. Fade-out start = length minus fade.")
            .video()
            .param("in", "Fade-in (s)", "1")
            .param("outstart", "Fade-out start (s)", "9")
            .param("out", "Fade-out (s)", "1")
            .step("-i <input> -vf \"fade=t=in:st=0:d={in},fade=t=out:st={outstart}:d={out}\" "
                + "-af \"afade=t=in:st=0:d={in},afade=t=out:st={outstart}:d={out}\" -c:v libx264 -c:a aac <output>")
            .build(),

        // --- SCALE, CROP, OVERLAY ----------------------------------------------------------
        new Builder("scale_exact", SCALE, "Scale to exact size")
            .orig("ffmpeg -i input.mp4 -vf \"scale=1280:720\" -c:a copy output.mp4")
            .video()
            .param("width", "Width", "1280")
            .param("height", "Height", "720")
            .step("-i <input> -vf \"scale={width}:{height}\" -c:a copy <output>")
            .build(),
        new Builder("scale_width", SCALE, "Scale by width, keep aspect ratio")
            .desc("-2 = even height (H.264 needs even dimensions).")
            .orig("ffmpeg -i input.mp4 -vf \"scale=640:-2\" output.mp4")
            .video()
            .param("width", "Width", "640")
            .step("-i <input> -vf \"scale={width}:-2\" <output>")
            .build(),
        new Builder("crop", SCALE, "Crop a region")
            .orig("ffmpeg -i input.mp4 -vf \"crop=640:480:100:50\" output.mp4")
            .video()
            .param("w", "Width", "640")
            .param("h", "Height", "480")
            .param("x", "X offset", "100")
            .param("y", "Y offset", "50")
            .step("-i <input> -vf \"crop={w}:{h}:{x}:{y}\" <output>")
            .build(),
        new Builder("watermark", SCALE, "Watermark (logo overlay)")
            .desc("Default position: top-right corner with a 10px margin.")
            .orig("ffmpeg -i input.mp4 -i logo.png -filter_complex \"overlay=W-w-10:10\" -c:a copy output.mp4")
            .video()
            .in("logo", "Logo image", "image/*")
            .param("pos", "Overlay position", "W-w-10:10")
            .step("-i <input> -i <logo> -filter_complex \"overlay={pos}\" -c:a copy <output>")
            .build(),
        new Builder("subtitles", SCALE, "Burn subtitles into the video")
            .desc("Uses libass with the system fonts.")
            .orig("ffmpeg -i input.mp4 -vf \"subtitles=subs.srt\" -c:a copy output.mp4")
            .video()
            .inCopy("subs", "Subtitle file (.srt/.ass)", "*/*")
            .step("-i <input> -vf \"subtitles=<subs>\" -c:a copy <output>")
            .build(),

        // --- PICTURE & COLOUR --------------------------------------------------------------
        new Builder("vertical_crop", PICTURE, "Vertical 9:16 (crop)")
            .desc("Fills a vertical frame by cropping the centre. For a square, use the same width and height.")
            .video()
            .param("width", "Width", "1080")
            .param("height", "Height", "1920")
            .step("-i <input> -vf \"scale={width}:{height}:force_original_aspect_ratio=increase,crop={width}:{height},setsar=1\" -c:a copy <output>")
            .build(),
        new Builder("vertical_pad", PICTURE, "Vertical 9:16 (black bars)")
            .desc("Keeps the whole picture inside a vertical frame, with bars.")
            .video()
            .param("width", "Width", "1080")
            .param("height", "Height", "1920")
            .param("color", "Bar colour", "black")
            .step("-i <input> -vf \"scale={width}:{height}:force_original_aspect_ratio=decrease,pad={width}:{height}:(ow-iw)/2:(oh-ih)/2:{color},setsar=1\" -c:a copy <output>")
            .build(),
        new Builder("vertical_blur", PICTURE, "Vertical 9:16 (blurred background)")
            .desc("Keeps the whole picture, over a blurred, zoomed copy of itself instead of bars.")
            .video()
            .param("width", "Width", "1080")
            .param("height", "Height", "1920")
            .step("-i <input> -filter_complex \"[0:v]split[a][b];[a]scale={width}:{height}:force_original_aspect_ratio=increase,crop={width}:{height},boxblur=20:5[bg];"
                + "[b]scale={width}:{height}:force_original_aspect_ratio=decrease[fg];[bg][fg]overlay=(W-w)/2:(H-h)/2,setsar=1[v]\" -map \"[v]\" -map 0:a? -c:a copy <output>")
            .build(),
        new Builder("rotate90", PICTURE, "Rotate 90°")
            .desc("1 = clockwise, 2 = counter-clockwise. Run twice for 180°.")
            .video()
            .param("dir", "Direction", "1")
            .step("-i <input> -vf transpose={dir} -c:a copy <output>")
            .build(),
        new Builder("mirror", PICTURE, "Mirror")
            .desc("hflip = left/right, vflip = upside down.")
            .video()
            .param("flip", "Flip", "hflip")
            .step("-i <input> -vf {flip} -c:a copy <output>")
            .build(),
        new Builder("straighten", PICTURE, "Straighten a tilted horizon")
            .desc("Rotates by a few degrees (negative = counter-clockwise) and zooms just enough to hide the corners.")
            .video()
            .param("degrees", "Angle (degrees)", "-2.5")
            .param("zoom", "Zoom", "1.08")
            .step("-i <input> -vf \"rotate={degrees}*PI/180,scale=iw*{zoom}:-2,crop=iw/{zoom}:ih/{zoom}\" -c:a copy <output>")
            .build(),
        new Builder("v360_flat", PICTURE, "360° video → flat view")
            .desc("Extracts a normal view from an equirectangular 360° video. Yaw turns left/right, pitch up/down.")
            .video()
            .param("yaw", "Yaw (degrees)", "0")
            .param("pitch", "Pitch (degrees)", "0")
            .param("hfov", "Horizontal field of view", "100")
            .param("vfov", "Vertical field of view", "60")
            .param("size", "Output size", "1920x1080")
            .step("-i <input> -vf \"v360=e:flat:yaw={yaw}:pitch={pitch}:h_fov={hfov}:v_fov={vfov},scale={size}\" -c:a copy <output>")
            .build(),
        new Builder("deinterlace", PICTURE, "Deinterlace (old footage)")
            .desc("Removes the comb lines of interlaced video (camcorders, TV recordings).")
            .video()
            .step("-i <input> -vf bwdif -c:a copy <output>")
            .build(),
        new Builder("denoise", PICTURE, "Reduce grain / noise")
            .desc("Smooths sensor noise. 4 = medium, 8 = strong.")
            .video()
            .param("strength", "Strength", "4")
            .step("-i <input> -vf hqdn3d={strength} -c:a copy <output>")
            .build(),
        new Builder("stabilize", PICTURE, "Stabilise shaky footage")
            .desc("Compensates small camera shakes (deshake filter).")
            .video()
            .step("-i <input> -vf deshake -c:a copy <output>")
            .build(),
        new Builder("sharpen", PICTURE, "Sharpen")
            .desc("Adds crispness. 1.0 = moderate, 2.0 = strong.")
            .video()
            .param("amount", "Amount", "1.0")
            .step("-i <input> -vf unsharp=5:5:{amount}:5:5:0 -c:a copy <output>")
            .build(),
        new Builder("color_correct", PICTURE, "Colour correction")
            .desc("Brightness (-1 to 1), contrast and saturation (1 = unchanged), white balance in Kelvin (6500 = neutral).")
            .video()
            .param("brightness", "Brightness", "0.05")
            .param("contrast", "Contrast", "1.1")
            .param("saturation", "Saturation", "1.15")
            .param("temperature", "White balance (K)", "6500")
            .step("-i <input> -vf \"eq=brightness={brightness}:contrast={contrast}:saturation={saturation},colortemperature=temperature={temperature}\" -c:a copy <output>")
            .build(),
        new Builder("lut", PICTURE, "Apply a 3D LUT (.cube)")
            .desc("Applies a colour grade or a Log → Rec.709 conversion from a .cube file.")
            .video()
            .inCopy("lut", "LUT file (.cube)", "*/*")
            .step("-i <input> -vf lut3d=<lut> -c:a copy <output>")
            .build(),
        new Builder("hdr_to_sdr", PICTURE, "HDR → SDR (washed-out iPhone video)")
            .desc("Tone-maps HDR (HLG on iPhone, PQ elsewhere) to normal Rec.709 so it looks right everywhere.")
            .video()
            .param("transfer", "Source curve (arib-std-b67 = HLG, smpte2084 = PQ)", "arib-std-b67")
            .param("tonemap", "Tone mapping (hable, mobius, reinhard)", "hable")
            .step("-i <input> -vf \"setparams=color_primaries=bt2020:color_trc={transfer}:colorspace=bt2020nc,zscale=t=linear:npl=100,format=gbrpf32le,zscale=p=bt709,tonemap=tonemap={tonemap}:desat=0,"
                + "zscale=t=bt709:m=bt709:r=tv,format=yuv420p\" -c:v libx264 -crf 18 -c:a copy <output>")
            .build(),
        new Builder("pixelate", PICTURE, "Pixelate a region (face, plate)")
            .desc("Pixelates a fixed rectangle: x, y = top-left corner, in pixels.")
            .video()
            .param("x", "X", "100")
            .param("y", "Y", "100")
            .param("w", "Width", "240")
            .param("h", "Height", "240")
            .param("block", "Block size", "16")
            .step("-i <input> -filter_complex \"[0:v]crop={w}:{h}:{x}:{y},scale=iw/{block}:-1,scale={w}:{h}:flags=neighbor[p];"
                + "[0:v][p]overlay={x}:{y}[v]\" -map \"[v]\" -map 0:a? -c:a copy <output>")
            .build(),
        new Builder("blur_region", PICTURE, "Blur a region")
            .desc("Blurs a fixed rectangle: x, y = top-left corner, in pixels.")
            .video()
            .param("x", "X", "100")
            .param("y", "Y", "100")
            .param("w", "Width", "240")
            .param("h", "Height", "240")
            .param("radius", "Blur radius", "12")
            .step("-i <input> -filter_complex \"[0:v]crop={w}:{h}:{x}:{y},boxblur={radius}[b];[0:v][b]overlay={x}:{y}[v]\" "
                + "-map \"[v]\" -map 0:a? -c:a copy <output>")
            .build(),
        new Builder("delogo", PICTURE, "Remove a fixed logo")
            .desc("Paints over a static logo or timestamp from the surrounding pixels.")
            .video()
            .param("x", "X", "20")
            .param("y", "Y", "20")
            .param("w", "Width", "180")
            .param("h", "Height", "60")
            .step("-i <input> -vf delogo=x={x}:y={y}:w={w}:h={h} -c:a copy <output>")
            .build(),

        // --- TEXT & OVERLAYS ---------------------------------------------------------------
        new Builder("title_text", TEXT, "Title text")
            .desc("Shows your text between two times (format hh:mm:ss,mmm). Position: 6 top, 10 middle, 2 bottom.")
            .video()
            .param("text", "Text", "Mon titre")
            .param("start", "From (hh:mm:ss,mmm)", "00:00:00,000")
            .param("end", "Until (hh:mm:ss,mmm)", "00:00:04,000")
            .param("align", "Position (6, 10 or 2)", "6")
            .param("size", "Size", "28")
            .gen("title", "title.srt", "1\n{start} --> {end}\n{text}\n")
            .step("-i <input> -vf \"subtitles=<title>:force_style='Alignment={align},FontSize={size},Bold=1,Outline=2,Shadow=1,MarginV=30'\" -c:a copy <output>")
            .build(),
        new Builder("lower_third", TEXT, "Lower third (name + role)")
            .desc("A name and a role in a box at the bottom left, between two times (format hh:mm:ss,mmm).")
            .video()
            .param("name", "Name", "Ada Lovelace")
            .param("role", "Role", "Mathématicienne")
            .param("start", "From (hh:mm:ss,mmm)", "00:00:01,000")
            .param("end", "Until (hh:mm:ss,mmm)", "00:00:06,000")
            .gen("lt", "lower_third.srt", "1\n{start} --> {end}\n{\\an1}<b>{name}</b>\n{role}\n")
            .step("-i <input> -vf \"subtitles=<lt>:force_style='FontSize=20,BorderStyle=3,Outline=6,BackColour=&H90000000,MarginL=40,MarginV=40'\" -c:a copy <output>")
            .build(),
        new Builder("meme", TEXT, "Meme text (top and bottom)")
            .desc("Big outlined text at the top and the bottom for the whole video.")
            .video()
            .param("top", "Top text", "QUAND FFMPEG")
            .param("bottom", "Bottom text", "MARCHE DU PREMIER COUP")
            .gen("meme", "meme.srt", "1\n00:00:00,000 --> 99:00:00,000\n{\\an8}{top}\n\n2\n00:00:00,000 --> 99:00:00,000\n{\\an2}{bottom}\n")
            .step("-i <input> -vf \"subtitles=<meme>:force_style='FontSize=26,Bold=1,Outline=3,MarginV=20'\" -c:a copy <output>")
            .build(),
        new Builder("pip", TEXT, "Picture-in-picture")
            .desc("Puts a second video (webcam, reaction) in the bottom-right corner; the main sound is kept.")
            .in("input", "Main video", "video/*")
            .in("pip", "Inset video", "video/*")
            .param("width", "Inset width", "480")
            .param("margin", "Margin", "24")
            .step("-i <input> -i <pip> -filter_complex \"[1:v]scale={width}:-2[p];[0:v][p]overlay=W-w-{margin}:H-h-{margin}:eof_action=pass[v]\" "
                + "-map \"[v]\" -map 0:a? -c:v libx264 -c:a copy <output>")
            .build(),
        new Builder("greenscreen", TEXT, "Green screen")
            .desc("Removes a green (or other) background from the foreground clip and puts it over the background.")
            .in("input", "Background (video or image)", "*/*")
            .in("fg", "Green-screen clip", "video/*")
            .param("color", "Key colour", "0x00FF00")
            .param("similarity", "Similarity", "0.25")
            .param("blend", "Edge softness", "0.08")
            .step("-i <input> -i <fg> -filter_complex \"[0:v][1:v]scale2ref[bg][f];[f]chromakey={color}:{similarity}:{blend}[k];"
                + "[bg][k]overlay=shortest=1,format=yuv420p[v]\" -map \"[v]\" -map 1:a? -c:v libx264 -c:a aac <output>")
            .build(),
        new Builder("subs_mux", TEXT, "Add switchable subtitles")
            .desc("Adds an .srt as a subtitle track players can turn on or off, without re-encoding.")
            .video()
            .in("subs", "Subtitle file (.srt)", "*/*")
            .param("lang", "Language code", "fre")
            .step("-i <input> -i <subs> -map 0 -map 1 -c copy -c:s mov_text -metadata:s:s:0 language={lang} <output>")
            .build(),

        // --- FRAMES & STREAMING ------------------------------------------------------------
        new Builder("poster", FRAMES, "Extract one frame (thumbnail)")
            .orig("ffmpeg -ss 00:00:05 -i input.mp4 -vframes 1 poster.jpg")
            .video()
            .param("time", "Time", "00:00:05")
            .step("-ss {time} -i <input> -vframes 1 <output>")
            .out("jpg", "image/jpeg")
            .build(),
        new Builder("frames", FRAMES, "Dump frames as JPEGs")
            .desc("frame_001.jpg, frame_002.jpg, … Save them to a folder afterwards.")
            .orig("ffmpeg -i input.mp4 -vf fps=1 frame_%03d.jpg")
            .video()
            .param("fps", "Frames per second", "1")
            .step("-i <input> -vf fps={fps} <outdir>/frame_%03d.jpg")
            .outDir()
            .build(),
        new Builder("rtmp", FRAMES, "Push to an RTMP server")
            .desc("-re = read at native frame rate. Keep the app open while streaming.")
            .orig("ffmpeg -re -i input.mp4 -c:v libx264 -preset veryfast -b:v 2500k -c:a aac -b:a 128k -f flv rtmp://live.example.com/app/STREAM_KEY")
            .video()
            .param("url", "RTMP URL", "rtmp://live.example.com/app/STREAM_KEY")
            .param("vbitrate", "Video bitrate", "2500k")
            .param("abitrate", "Audio bitrate", "128k")
            .step("-re -i <input> -c:v libx264 -preset veryfast -b:v {vbitrate} -c:a aac -b:a {abitrate} -f flv {url}")
            .noOut()
            .build(),

        // --- ANALYSIS ----------------------------------------------------------------------
        new Builder("info", ANALYSIS, "Media info")
            .desc("Duration, streams, codecs, resolution, frame rate, bitrates and tags, in the log.")
            .media()
            .step("-i <input> -map 0 -c copy -f null -")
            .report()
            .build(),
        new Builder("detect_bars", ANALYSIS, "Detect black bars")
            .desc("Reports the crop that removes black borders (last crop= line in the log).")
            .video()
            .param("seconds", "Seconds to analyse", "20")
            .step("-i <input> -t {seconds} -vf cropdetect -an -f null -")
            .report()
            .build(),
        new Builder("detect_scenes", ANALYSIS, "Detect scene changes")
            .desc("Lists the times of cuts (pts_time in the log). Lower threshold = more cuts.")
            .video()
            .param("threshold", "Threshold (0-1)", "0.4")
            .step("-i <input> -vf \"select='gt(scene,{threshold})',showinfo\" -an -f null -")
            .report()
            .build(),
        new Builder("measure_loudness", ANALYSIS, "Measure loudness (EBU R128)")
            .desc("Integrated loudness (LUFS), range and true peak: the summary is at the end of the log.")
            .media()
            .step("-i <input> -vn -af ebur128=peak=true -f null -")
            .report()
            .build(),
        new Builder("detect_silence", ANALYSIS, "Find silences")
            .desc("Lists silence_start / silence_end times, e.g. to plan cuts.")
            .media()
            .param("threshold", "Silence level (dB)", "-40")
            .param("min", "Minimum length (s)", "0.8")
            .step("-i <input> -vn -af silencedetect=noise={threshold}dB:d={min} -f null -")
            .report()
            .build(),
        new Builder("compare_quality", ANALYSIS, "Compare quality (SSIM / PSNR)")
            .desc("Measures how close an encode is to its original (same size and frame rate). SSIM 1 = identical.")
            .in("input", "Encoded video", "video/*")
            .in("reference", "Original video", "video/*")
            .step("-i <input> -i <reference> -lavfi \"[0:v]split[a0][a1];[1:v]split[b0][b1];[a0][b0]ssim;[a1][b1]psnr\" -f null -")
            .report()
            .build(),

        // --- CUSTOM ------------------------------------------------------------------------
        new Builder("custom", CUSTOM, "Custom command")
            .desc("Edit the command freely. <input> is the picked file, <output> the result file.")
            .video()
            .param("format", "Output extension", "mp4")
            .step("-i <input> -c:v libx264 -c:a aac <output>")
            .out("{format}", "*/*")
            .build()
    );

    public static Recipe byId(String id) {
        for (Recipe r : ALL) if (r.id.equals(id)) return r;
        return null;
    }
}
