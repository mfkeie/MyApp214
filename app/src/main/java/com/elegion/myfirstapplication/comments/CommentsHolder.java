package com.elegion.myfirstapplication.comments;

import android.support.v7.widget.RecyclerView;
import android.view.View;
import android.widget.TextView;

import com.elegion.myfirstapplication.R;
import com.elegion.myfirstapplication.model.Comment;

public class CommentsHolder extends RecyclerView.ViewHolder {

    private TextView mAuthor;
    private TextView mText;
    private TextView mTimeStamp;

    public CommentsHolder(View itemView) {
        super(itemView);
        mAuthor = itemView.findViewById(R.id.tv_author);
        mText = itemView.findViewById(R.id.tv_text);
        mTimeStamp = itemView.findViewById(R.id.tv_timestamp);
    }

    public void bind(Comment item) {
        mAuthor.setText(/*item.getId() + ": " + */item.getAuthor());
        mText.setText(item.getText());
        //TODO: изменить вывод даты
        mTimeStamp.setText(item.getTimestamp());
    }
}
