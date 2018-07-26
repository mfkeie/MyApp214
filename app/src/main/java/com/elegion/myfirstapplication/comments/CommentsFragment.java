package com.elegion.myfirstapplication.comments;

import android.app.Activity;
import android.arch.persistence.room.util.StringUtil;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.text.TextUtils;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.elegion.myfirstapplication.ApiUtils;
import com.elegion.myfirstapplication.App;
import com.elegion.myfirstapplication.R;
import com.elegion.myfirstapplication.db.MusicDao;
import com.elegion.myfirstapplication.model.Album;
import com.elegion.myfirstapplication.model.Comment;

import org.w3c.dom.Text;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;

import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.annotations.NonNull;
import io.reactivex.schedulers.Schedulers;
import okhttp3.internal.Util;

public class CommentsFragment extends Fragment implements SwipeRefreshLayout.OnRefreshListener {

    private static final String ALBUM_KEY = "ALBUM_KEY";

    private RecyclerView mRecyclerView;
    private SwipeRefreshLayout mRefresher;
    private View mErrorCommentsView;
    private Album mAlbum;
    private RelativeLayout mComments;
    private Button mSendButton;
    private EditText mCommentEditText;
    private List<Comment> mCoommentList;
    private boolean firstLoad = true;
    private String mCommentUrl  = "";

    @NonNull
    private final CommentsAdapter mCommentAdapter = new CommentsAdapter();

    public static CommentsFragment newInstance(Album album) {
        Bundle args = new Bundle();
        args.putSerializable(ALBUM_KEY, album);

        CommentsFragment fragment = new CommentsFragment();
        fragment.setArguments(args);

        return fragment;
    }

    @Nullable
    @Override
    public View onCreateView(@android.support.annotation.NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fr_recycler_comments, container, false);
    }

    @Override
    public void onViewCreated(@android.support.annotation.NonNull View view, @Nullable Bundle savedInstanceState) {
        mRecyclerView = view.findViewById(R.id.recycler_comments);
        mRefresher = view.findViewById(R.id.refresher_comments);
        mRefresher.setOnRefreshListener(this);
        mErrorCommentsView = view.findViewById(R.id.errorCommentsView);
        mComments = view.findViewById(R.id.rl_comments);
        mSendButton = view.findViewById(R.id.button_send);
        mCommentEditText = view.findViewById(R.id.et_comment);

        mSendButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                hideKeyboard(getActivity());
                sendComment();
            }
        });

       /* mCommentEditText.setOnKeyListener(new View.OnKeyListener() {
            @Override
            public boolean onKey(View v, int keyCode, KeyEvent event) {
                if (event.getAction() == KeyEvent.ACTION_DOWN &&
                        keyCode == KeyEvent.KEYCODE_ENTER){

                }
        );*/

        mCommentEditText.setOnKeyListener(new View.OnKeyListener() {
            @Override
            public boolean onKey(View v, int keyCode, KeyEvent event) {
                boolean handled = false;
                if (event.getAction() == KeyEvent.ACTION_DOWN &&
                        keyCode == KeyEvent.KEYCODE_ENTER) {
                    hideKeyboard(getActivity());
                    sendComment();
                    handled = false;
                }
                return handled;
            }
        });


        /*mCommentEditText.setOnEditorActionListener(new TextView.OnEditorActionListener() {
            @Override
            public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
                boolean handled = false;
                if (event.getAction() == KeyEvent.ACTION_DOWN &&
                        event.getAction() == KeyEvent.KEYCODE_ENTER) {
                    hideKeyboard(getActivity());
                    sendComment();
                    handled = false;
                }
                return handled;
            }
        });*/
    }

    public static void hideKeyboard(Activity activity) {
        InputMethodManager imm = (InputMethodManager) activity.getSystemService(Activity.INPUT_METHOD_SERVICE);
        //Find the currently focused view, so we can grab the correct window token from it.
        View view = activity.getCurrentFocus();
        //If no view currently has focus, create a new one, just so we can grab a window token from it
        if (view == null) {
            view = new View(activity);
        }
        imm.hideSoftInputFromWindow(view.getWindowToken(), 0);
    }

    private void sendComment() {
        String commentText = mCommentEditText.getText().toString();

        if(!TextUtils.isEmpty(commentText)) {
            Comment comment = new Comment();
            comment.setAlbumId(mAlbum.getId());
            comment.setText(commentText);
            mCommentUrl = "";

            mRefresher.setRefreshing(true);
            ApiUtils.getApiService()
                    .comments(comment)
                    .subscribeOn(Schedulers.io())
                    // .doOnSubscribe(disposable -> mRefresher.setRefreshing(true))
                    // .doFinally(() -> mRefresher.setRefreshing(false))
                    .observeOn(AndroidSchedulers.mainThread())
                    .subscribe(
                            commentResponse -> {
                                mCommentEditText.setText("");
                                mCommentUrl = commentResponse.headers().get("Location");
                                getComment(Integer.parseInt(mCommentUrl.substring(mCommentUrl.lastIndexOf("/") + 1)));

                            }, throwable -> {
                                Toast.makeText(getActivity(), throwable.getMessage(), Toast.LENGTH_LONG).show();
                                mRefresher.setRefreshing(false);
                            });

            if(!TextUtils.isEmpty(mCommentUrl)) {

            }


        } else {
            Toast.makeText(getActivity(), "нет текста для отправки", Toast.LENGTH_LONG).show();
        }
    }

    @Override
    public void onActivityCreated(@Nullable Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

        mAlbum = (Album) getArguments().getSerializable(ALBUM_KEY);

        getActivity().setTitle(mAlbum.getName());

        mRecyclerView.setLayoutManager(new LinearLayoutManager(getActivity()));
        mRecyclerView.setAdapter(mCommentAdapter);

        onRefresh();
    }

    @Override
    public void onRefresh() {
        mRefresher.post(() -> {
            mRefresher.setRefreshing(true);
            getComments();
        });
    }

    private void getComments() {
        ApiUtils.getApiService()
                .getAlbumComments(mAlbum.getId())
                .subscribeOn(Schedulers.io())
                /*.doOnSuccess(
                        album -> {
                            getMusicDao().insertAlbum(album);
                            getMusicDao().insertSongs(album.getSongs());
                            for(Song song : album.getSongs()) {
                                getMusicDao().setLinkAlbumSongs(new AlbumSong(album.getId(), song.getId()));
                            }
                        }

                )
                .onErrorReturn(throwable -> {
                    if (ApiUtils.NETWORK_EXCEPTIONS.contains(throwable.getClass())) {
                        Album album = getMusicDao().getAlbumWithId(mAlbum.getId());
                        List<Song> songs = getMusicDao().getSongsFromAlbum(mAlbum.getId());
                        album.setSongs(songs);
                        return album;
                    } else return null;
                })*/
                .observeOn(AndroidSchedulers.mainThread())
                .doOnSubscribe(disposable -> mRefresher.setRefreshing(true))
                .doFinally(() -> mRefresher.setRefreshing(false))
                .subscribe(
                        comments -> {
                            mErrorCommentsView.setVisibility(View.GONE);
                            mComments.setVisibility(View.VISIBLE);
                            mRecyclerView.setVisibility(View.VISIBLE);
                            if(firstLoad)
                                mCommentAdapter.addData(comments, true);
                            if(!firstLoad) {
                                if(!isCollectionMatch(mCoommentList, comments)) {
                                    Toast.makeText(getActivity(), "Комментарии обновлены", Toast.LENGTH_LONG).show();
                                    mCommentAdapter.addData(comments, true);
                                }
                                else
                                    Toast.makeText(getActivity(), "Новых комментариев нет", Toast.LENGTH_LONG).show();
                            }
                            mCoommentList = comments;
                            firstLoad = false;
                        },
                        throwable -> {
                            mErrorCommentsView.setVisibility(View.VISIBLE);
                            mRecyclerView.setVisibility(View.GONE);
                            mComments.setVisibility(View.GONE);
                            mCoommentList = null;
                        });
    }

    private void getComment(int id) {
        ApiUtils.getApiService()
                .getComment(id)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                //.doOnSubscribe(disposable -> mRefresher.setRefreshing(true))
                //.doFinally(() -> mRefresher.setRefreshing(false))
                .subscribe(
                        comment -> {
                            mErrorCommentsView.setVisibility(View.GONE);
                            mComments.setVisibility(View.VISIBLE);
                            mRecyclerView.setVisibility(View.VISIBLE);
                            mCommentAdapter.addData(comment);
                            mRecyclerView.smoothScrollToPosition(mCommentAdapter.getItemCount());
                            mRefresher.setRefreshing(false);
                        },
                        throwable -> {
                            mErrorCommentsView.setVisibility(View.VISIBLE);
                            mRecyclerView.setVisibility(View.GONE);
                            mComments.setVisibility(View.GONE);
                            mRefresher.setRefreshing(false);
                        });
    }


    private MusicDao getMusicDao() {
        return ((App) getActivity().getApplication()).getDatabase().getMusicDao();
    }

    public static <T> boolean isCollectionMatch(Collection<T> one, Collection<T> two) {
        if (one == two)
            return true;

        // If either list is null, return whether the other is empty
        if (one == null)
            return two.isEmpty();
        if (two == null)
            return one.isEmpty();

        // If lengths are not equal, they can't possibly match
        if (one.size() != two.size())
            return false;

        // copy the second list, so it can be modified
        final List<T> ctwo = new ArrayList<>(two);

        for (T itm : one) {
            Iterator<T> it = ctwo.iterator();
            boolean gotEq = false;
            while (it.hasNext()) {
                if (itm.equals(it.next())) {
                    it.remove();
                    gotEq = true;
                    break;
                }
            }
            if (!gotEq) return false;
        }
        // All elements in one were found in two, and they're the same size.
        return true;
    }


}
