package app.sonu.com.musicplayer.ui.main;

import app.sonu.com.musicplayer.base.ui.BaseMvpView;

/**
 * Created by sonu on 29/6/17.
 */

public interface MainMvpView extends BaseMvpView {
    void setSlidingUpPaneCollapsed();
    void setSlidingUpPaneExpanded();
    void setSlidingUpPaneHidden();
    boolean isSlidingUpPaneHidden();
    void hideMiniPlayer();
    void showMiniPlayer();
}