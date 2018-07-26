package com.elegion.myfirstapplication.comments;

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
import android.widget.Button;
import android.widget.EditText;
import android.widget.RelativeLayout;
import android.widget.Toast;

import com.elegion.myfirstapplication.ApiUtils;
import com.elegion.myfirstapplication.App;
import com.elegion.myfirstapplication.R;
import com.elegion.myfirstapplication.common.Utils;
import com.elegion.myfirstapplication.db.MusicDao;
import com.elegion.myfirstapplication.model.Album;
import com.elegion.myfirstapplication.model.Comment;
import java.util.List;

import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.annotations.NonNull;
import io.reactivex.schedulers.Schedulers;

public class CommentsFragment extends Fragment implements SwipeRefreshLayout.OnRefreshListener {

    private static final String ALBUM_KEY = "ALBUM_KEY";

    private RecyclerView mRecyclerView;
    private SwipeRefreshLayout mRefresher;
    private View mErrorCommentsView;
    private Album mAlbum;
    private RelativeLayout mComments;
    private Button mSendButton;
    private EditText mCommentEditText;
    private List<Comment> mCommentList;
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
                Utils.hideKeyboard(getActivity());
                sendComment();
            }
        });

        mCommentEditText.setOnKeyListener(new View.OnKeyListener() {
            @Override
            public boolean onKey(View v, int keyCode, KeyEvent event) {
                boolean handled = false;
                if (event.getAction() == KeyEvent.ACTION_DOWN &&
                        keyCode == KeyEvent.KEYCODE_ENTER) {
                    Utils.hideKeyboard(getActivity());
                    sendComment();
                    handled = false;
                }
                return handled;
            }
        });
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

    /**
     * Отправка комментария
     */
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
        } else {
            Toast.makeText(getActivity(), "нет текста для отправки", Toast.LENGTH_LONG).show();
        }
    }

    /**
     * Получение комментариев
     */
    private void getComments() {
        ApiUtils.getApiService()
                .getAlbumComments(mAlbum.getId())
                .subscribeOn(Schedulers.io())
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
                                if(!Utils.isCollectionEquals(mCommentList, comments)) {
                                    Toast.makeText(getActivity(), "Комментарии обновлены", Toast.LENGTH_LONG).show();
                                    mCommentAdapter.addData(comments, true);
                                }
                                else
                                    Toast.makeText(getActivity(), "Новых комментариев нет", Toast.LENGTH_LONG).show();
                            }
                            mCommentList = comments;
                            firstLoad = false;
                        },
                        throwable -> {
                            mErrorCommentsView.setVisibility(View.VISIBLE);
                            mRecyclerView.setVisibility(View.GONE);
                            mComments.setVisibility(View.GONE);
                           // mCoommentList = null;
                        });
    }

    /**
     * Получить комментарий
     * @param id ид комментария
     */
    private void getComment(int id) {
        ApiUtils.getApiService()
                .getComment(id)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
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
}
