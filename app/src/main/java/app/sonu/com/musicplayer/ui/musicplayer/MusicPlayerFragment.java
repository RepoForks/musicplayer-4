package app.sonu.com.musicplayer.ui.musicplayer;

import android.content.ComponentName;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.graphics.PorterDuff;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.RemoteException;
import android.os.SystemClock;
import android.support.annotation.Nullable;
import android.support.v4.media.MediaBrowserCompat;
import android.support.v4.media.MediaMetadataCompat;
import android.support.v4.media.session.MediaControllerCompat;
import android.support.v4.media.session.MediaSessionCompat;
import android.support.v4.media.session.PlaybackStateCompat;
import android.text.format.DateUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.SeekBar;
import android.widget.TextView;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.DataSource;
import com.bumptech.glide.load.engine.GlideException;
import com.bumptech.glide.request.RequestListener;
import com.bumptech.glide.request.RequestOptions;
import com.bumptech.glide.request.target.Target;
import com.sothree.slidinguppanel.SlidingUpPanelLayout;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

import app.sonu.com.musicplayer.MyApplication;
import app.sonu.com.musicplayer.R;
import app.sonu.com.musicplayer.base.ui.BaseFragment;
import app.sonu.com.musicplayer.data.db.model.Song;
import app.sonu.com.musicplayer.di.component.DaggerUiComponent;
import app.sonu.com.musicplayer.di.module.UiModule;
import app.sonu.com.musicplayer.mediaplayernew.MusicService;
import app.sonu.com.musicplayer.ui.main.SlidingUpPaneCallback;
import app.sonu.com.musicplayer.utils.ColorUtil;
import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;

/**
 * Created by sonu on 4/7/17.
 */

public class MusicPlayerFragment extends BaseFragment<MusicPlayerMvpPresenter>
        implements MusicPlayerMvpView {

    String TAG = MusicPlayerFragment.class.getSimpleName();

    private static final long PROGRESS_UPDATE_INTERNAL = 1000;
    private static final long PROGRESS_UPDATE_INITIAL_INTERVAL = 100;
    private final Handler mHandler = new Handler();
    private final Runnable mUpdateProgressTask = new Runnable() {
        @Override
        public void run() {
            mPresenter.updateProgress();
        }
    };

    private final ScheduledExecutorService mExecutorService =
            Executors.newSingleThreadScheduledExecutor();

    private ScheduledFuture<?> mScheduleFuture;

    @BindView(R.id.musicPlayerParentLl)
    View musicPlayerParent;

    @BindView(R.id.playPauseIb)
    ImageButton playPauseIb;

    @BindView(R.id.skipNextIb)
    ImageButton skipNextIb;

    @BindView(R.id.skipPreviousIb)
    ImageButton skipPreviousButton;

    @BindView(R.id.songTitleMainTv)
    TextView songTitleMainTv;

    @BindView(R.id.songArtistMainTv)
    TextView songArtistMainTv;

    @BindView(R.id.songCurrentPositionSeekBar)
    SeekBar songCurrentPositionSeekBar;

    @BindView(R.id.elapsedTimeTv)
    TextView elapsedTimeTv;

    @BindView(R.id.totalTimeTv)
    TextView totalTimeTv;

    @BindView(R.id.musicPlayerSupl)
    SlidingUpPanelLayout musicPlayerSupl;

    @BindView(R.id.musicPlayerUpperHalfRl)
    View musicPlayerUpperHalfRl;

    @BindView(R.id.shuffleIb)
    ImageButton shuffleIb;

    @BindView(R.id.repeatIb)
    ImageButton repeatIb;

    @OnClick(R.id.playPauseIb)
    void onPlayPauseIbClick(){
        Log.d(TAG, "playPauseIb onClick:called");
        mPresenter.playPauseButtonOnClick();
    }

    @OnClick(R.id.skipNextIb)
    void onSkipNextIbClick(){
        Log.d(TAG, "skipNextIb onClick:called");
        mPresenter.skipNextButtonOnClick();
    }

    @OnClick(R.id.skipPreviousIb)
    void onSkipPreviousIbClick(){
        Log.d(TAG, "skipPreviousIb onClick:called");
        mPresenter.skipPreviousButtonOnClick();
    }

    @OnClick(R.id.shuffleIb)
    void onShuffleIbClick() {
        mPresenter.onShuffleButtonClick();
    }

    @OnClick(R.id.repeatIb)
    void onRepeatIbClick() {
        mPresenter.onRepeatButtonClick();
    }

    @BindView(R.id.albumArtIv)
    ImageView albumArtIv;

    @OnClick(R.id.collapseIv)
    void onCollapseIvClick() {
        mPresenter.onCollapseIvClick();
    }

    @BindView(R.id.collapseIv)
    ImageView collapseIv;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        DaggerUiComponent.builder()
                .uiModule(new UiModule(getActivity()))
                .applicationComponent(((MyApplication)getActivity().getApplicationContext())
                        .getApplicationComponent())
                .build()
                .inject(this);

        Log.d(TAG, "onCreate:is presenter null="+(mPresenter==null));

        mPresenter.onCreate(getActivity());
    }

    @Override
    public void onStart() {
        super.onStart();
    }

    @Override
    public void onStop() {
        super.onStop();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        Log.d(TAG, "onDestroy:called");
        mPresenter.onDestroy();
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_music_player, container, false);
        ButterKnife.bind(this, view);

        mPresenter.onCreateView();

        ((SlidingUpPaneCallback) getActivity()).setDragView(musicPlayerUpperHalfRl);

        musicPlayerSupl.addPanelSlideListener(new SlidingUpPanelLayout.PanelSlideListener() {
            @Override
            public void onPanelSlide(View panel, float slideOffset) {

            }

            @Override
            public void onPanelStateChanged(View panel,
                                            SlidingUpPanelLayout.PanelState previousState,
                                            SlidingUpPanelLayout.PanelState newState) {
                if (newState == SlidingUpPanelLayout.PanelState.EXPANDED ||
                        newState == SlidingUpPanelLayout.PanelState.DRAGGING) {
                    //todo temp
                    ((SlidingUpPaneCallback) getActivity()).setDragViewNow(null);
                } else {
                    //todo temp
                    ((SlidingUpPaneCallback) getActivity()).setDragViewNow(musicPlayerUpperHalfRl);
                }
            }
        });

        songCurrentPositionSeekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                elapsedTimeTv.setText(DateUtils.formatElapsedTime(progress / 1000));
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {
                stopSeekbarUpdate();
            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
                mPresenter.onSeekbarStopTrackingTouch(seekBar.getProgress());
            }
        });

        return view;
    }

    @Override
    public void onResume() {
        super.onResume();

        Log.d(TAG, "onResume:is presenter's view null="+(mPresenter.getMvpView()==null));
    }

    @Override
    public void displaySong(String songTitle,
                            String songSubtitle,
                            String songDuration,
                            String albumArtPath) {
        songTitleMainTv.setText(songTitle);
        songArtistMainTv.setText(songSubtitle);

        Glide.with(getActivity()).clear(albumArtIv);

        RequestOptions options = new RequestOptions();
        options.centerCrop();

        if (albumArtPath != null) {
            Glide.with(getActivity())
                    .asBitmap()
                    .load(albumArtPath)
                    .apply(options)
                    .listener(new RequestListener<Bitmap>() {
                        @Override
                        public boolean onLoadFailed(@Nullable GlideException e,
                                                    Object model,
                                                    Target<Bitmap> target,
                                                    boolean isFirstResource) {
                            return false;
                        }

                        @Override
                        public boolean onResourceReady(Bitmap resource,
                                                       Object model,
                                                       Target<Bitmap> target,
                                                       DataSource dataSource,
                                                       boolean isFirstResource) {

//                                    collapseIv.setColorFilter(
//                                            ColorUtil.getColor(
//                                                    ColorUtil.generatePalette(resource),
//                                                    Color.DKGRAY),
//                                            PorterDuff.Mode.SRC_IN);
                            return false;
                        }
                    })
                    .into(albumArtIv);
        } else {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                albumArtIv.setImageDrawable(getActivity()
                        .getDrawable(R.drawable.default_album_art_note_big));
            } else {
                albumArtIv.setImageDrawable(
                        getActivity()
                                .getResources()
                                .getDrawable(R.drawable.default_album_art_note_big));
            }
        }

        totalTimeTv.setText(songDuration);
    }

    @Override
    public void showPlayIcon() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            playPauseIb.setImageDrawable(getResources()
                    .getDrawable(R.drawable.ic_play_arrow_accent_48dp, null));
        } else {
            playPauseIb.setImageDrawable(getResources()
                    .getDrawable(R.drawable.ic_play_arrow_accent_48dp));
        }
    }

    @Override
    public void showPauseIcon() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            playPauseIb.setImageDrawable(getResources()
                    .getDrawable(R.drawable.ic_pause_accent_48dp, null));
        } else {
            playPauseIb.setImageDrawable(getResources()
                    .getDrawable(R.drawable.ic_pause_accent_48dp));
        }
    }

    @Override
    public void setSeekBarPosition(int position) {
        songCurrentPositionSeekBar.setProgress(position);
    }

    @Override
    public void setElapsedTime(int position) {
        elapsedTimeTv.setText(DateUtils.formatElapsedTime(position/1000));
    }

    @Override
    public void updateDuration(long dur) {
        Log.d(TAG, "updateDuration:called");
        int duration = (int) dur;
        songCurrentPositionSeekBar.setMax(duration);
        totalTimeTv.setText(DateUtils.formatElapsedTime(duration/1000));

        Log.i(TAG, "updateDuration:total="+DateUtils.formatElapsedTime(duration/1000));
    }

    @Override
    public void scheduleSeekbarUpdate() {
        Log.d(TAG, "scheduleSeekbarUpdate:called");
        stopSeekbarUpdate();
        if (!mExecutorService.isShutdown()) {
            Log.d(TAG, "scheduleSeekbarUpdate:isnotshutdown");
            mScheduleFuture = mExecutorService.scheduleAtFixedRate(
                    new Runnable() {
                        @Override
                        public void run() {
                            mHandler.post(mUpdateProgressTask);
                        }
                    }, PROGRESS_UPDATE_INITIAL_INTERVAL,
                    PROGRESS_UPDATE_INTERNAL, TimeUnit.MILLISECONDS);
        }
    }

    @Override
    public void stopSeekbarUpdate() {
        if (mScheduleFuture != null) {
            mScheduleFuture.cancel(false);
        }
    }

    @Override
    public void setShuffleModeEnabled() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            shuffleIb
                    .setImageDrawable(
                            getActivity().getDrawable(R.drawable.ic_shuffle_grey_24dp));
        } else {
            shuffleIb.setImageDrawable(
                    getActivity()
                            .getResources()
                            .getDrawable(R.drawable.ic_shuffle_grey_24dp));
        }
    }

    @Override
    public void setShuffleModeDisabled() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            shuffleIb
                    .setImageDrawable(
                            getActivity().getDrawable(R.drawable.ic_shuffle_light_grey_24dp));
        } else {
            shuffleIb.setImageDrawable(
                    getActivity()
                            .getResources()
                            .getDrawable(R.drawable.ic_shuffle_light_grey_24dp));
        }
    }
}