<b> MusicPlayer </b>

Music player is an Android application that plays audio files in MP3 format from mobile storage.

It consists of two main application screens, first one is library where all audio files are listed and user can select any song from the list.
Song selection opens the second screen in which user can use audio controls like play, stop, forward, backward, previous, next. All of listed controls are also available via voice
recognition by holding the microphone button. User can also go back to the library by clicking Library icon on bottom left corner of navigation menu.

The application uses WaveRecorder API for recording audio in WAVE format with the corresponding header and Tensorflow Lite API to open tflite model and run inference on the recorded audio file.
For opening audio files and generating MFCC features jLibrosa library is used which is a Java implementation of librosa python API that is used for training the model.
