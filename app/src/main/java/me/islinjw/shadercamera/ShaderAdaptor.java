package me.islinjw.shadercamera;

import android.support.annotation.NonNull;
import android.support.annotation.StringRes;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import java.util.List;

import me.islinjw.shadercamera.gl.shader.IShader;

public class ShaderAdaptor extends RecyclerView.Adapter<ShaderAdaptor.Holder> implements
        View.OnClickListener {
    private RecyclerView mRecyclerView;
    private List<ShaderInfo> mShaders;
    private OnSelectShaderListener mListener;
    private int mSelectPosition = 0;

    public ShaderAdaptor(RecyclerView recyclerView, List<ShaderInfo> shaders) {
        mRecyclerView = recyclerView;
        mShaders = shaders;
    }

    @NonNull
    @Override
    public Holder onCreateViewHolder(@NonNull ViewGroup viewGroup, int i) {
        View item = LayoutInflater.from(viewGroup.getContext())
                .inflate(R.layout.shader_selector_item, viewGroup, false);
        item.setOnClickListener(this);
        return new Holder(item);
    }

    @Override
    public void onBindViewHolder(@NonNull Holder holder, int position) {
        holder.mShaderName.setText(mShaders.get(position).getName());
        holder.itemView.setSelected(mSelectPosition == position);

    }

    @Override
    public int getItemCount() {
        return mShaders.size();
    }

    public void setOnSelectShaderListener(OnSelectShaderListener listener) {
        mListener = listener;
    }

    @Override
    public void onClick(View v) {
        int newSelect = mRecyclerView.getChildAdapterPosition(v);
        if (mSelectPosition != newSelect) {
            int oldSelect = mSelectPosition;
            mSelectPosition = newSelect;
            notifyItemChanged(oldSelect);
            notifyItemChanged(newSelect);
        }

        if (mListener != null) {
            ShaderInfo shaderInfo = mShaders.get(mRecyclerView.getChildAdapterPosition(v));
            mListener.onSelectShader(shaderInfo.getShader());
        }
    }

    public static class Holder extends RecyclerView.ViewHolder {
        private TextView mShaderName;

        public Holder(@NonNull View itemView) {
            super(itemView);
            mShaderName = itemView.findViewById(R.id.shader_name);
        }
    }

    public static class ShaderInfo {
        private int mName;

        private Class<? extends IShader> mClass;

        private IShader mShader;

        public ShaderInfo(@StringRes int name, @NonNull Class<? extends IShader> clazz) {
            mName = name;
            mClass = clazz;
        }

        public IShader getShader() {
            if (mShader == null) {
                try {
                    mShader = mClass.newInstance();
                } catch (Exception e) {
                    throw new RuntimeException("can not create shader " + mName, e);
                }
            }
            return mShader;
        }

        public int getName() {
            return mName;
        }
    }

    public interface OnSelectShaderListener {
        void onSelectShader(IShader shader);
    }
}
