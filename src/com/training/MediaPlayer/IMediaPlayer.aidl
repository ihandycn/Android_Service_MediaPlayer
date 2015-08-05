package com.training.MediaPlayer;

import android.net.Uri;

/**
 * Created with IntelliJ IDEA.
 * User: ruinisem
 * Date: 9/8/13
 * Time: 12:09 AM
 * To change this template use File | Settings | File Templates.
 */
interface IMediaPlayer {

    void play(in Uri uri);

    void pause();

    void resume();

    void stop();
}
