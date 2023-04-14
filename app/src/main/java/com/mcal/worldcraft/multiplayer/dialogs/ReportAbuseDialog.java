package com.mcal.worldcraft.multiplayer.dialogs;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.Dialog;
import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.TextView;

import androidx.annotation.NonNull;

import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.mcal.worldcraft.Enemy;
import com.mcal.worldcraft.Persistence;
import com.mcal.worldcraft.R;
import com.mcal.worldcraft.factories.DescriptionFactory;
import com.mcal.worldcraft.multiplayer.Multiplayer;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;

public class ReportAbuseDialog {
    private final Context context;
    private Enemy selectedEnemy;

    public ReportAbuseDialog(Context context) {
        this.context = context;
    }

    public void show() {
        final Dialog dialog = new Dialog(this.context);
        dialog.setContentView(R.layout.report_abuse_dialog);
        dialog.setTitle(R.string.report_abuse);
        initPlayerList(dialog);
        initMessageList(dialog);
        final EditText abuseEditText = dialog.findViewById(R.id.abuse_text);
        Button sendAbuseButton = dialog.findViewById(R.id.send_button);
        sendAbuseButton.setOnClickListener(v -> {
            String abuseText = String.valueOf(abuseEditText.getText()).trim();
            if (ReportAbuseDialog.this.selectedEnemy == null) {
                PopupDialog.show(R.string.error, R.string.please_choose_player, dialog.getContext());
            } else if (!DescriptionFactory.emptyText.equals(abuseText)) {
                ReportAbuseDialog.this.showAreYouShureDialog(dialog, ReportAbuseDialog.this.selectedEnemy.id, abuseText);
            } else {
                PopupDialog.show(R.string.error, R.string.please_enter_report_description, dialog.getContext());
            }
        });
        Button cancelButton = dialog.findViewById(R.id.cancel_button);
        cancelButton.setOnClickListener(v -> dialog.dismiss());
        dialog.getWindow().setSoftInputMode(2);
        dialog.show();
    }

    protected void showAreYouShureDialog(@NonNull final Dialog parentDialog, final int playerId, final String abuseText) {
        final MaterialAlertDialogBuilder alertDialogBuilder = PopupDialog.createDialog(R.string.report_abuse, R.string.are_you_sure_you_want_to_send_this_report, parentDialog.getContext());
        if (alertDialogBuilder != null) {
            alertDialogBuilder.setPositiveButton(android.R.string.ok, (dialog, which) -> {
                Multiplayer.reportAbuse(playerId, abuseText);
                dialog.dismiss();
                parentDialog.dismiss();
            });
            alertDialogBuilder.setNegativeButton(android.R.string.cancel, (dialog, which) -> dialog.dismiss());
            alertDialogBuilder.show();
        }
    }

    private void initPlayerList(final Dialog dialog) {
        List<Enemy> sortedEnemies;
        if (Multiplayer.instance != null && (sortedEnemies = new ArrayList<>(Multiplayer.getEnemiesCopy())) != null) {
            Collections.sort(sortedEnemies, Enemy.nameComparator);
            final PlayerListItem[] playerItems = new PlayerListItem[sortedEnemies.size()];
            int index = 0;
            for (Enemy enemy : sortedEnemies) {
                playerItems[index] = new PlayerListItem(Persistence.getSkinResID(enemy.skin), enemy.name);
                index++;
            }
            final PlayerAdapter adapter = new PlayerAdapter(this.context, R.layout.report_abuse_player_list_item, playerItems);
            ListView playerList = dialog.findViewById(R.id.player_list);
            playerList.setAdapter(adapter);
            playerList.setOnItemClickListener(new AdapterView.OnItemClickListener() {
                @Override
                public void onItemClick(AdapterView<?> arg0, View arg1, int position, long arg3) {
                    ReportAbuseDialog.this.onPlayerClick(dialog, playerItems[position]);
                    deselectPlayers();
                    playerItems[position].isSelected = true;
                    adapter.notifyDataSetChanged();
                }

                private void deselectPlayers() {
                    for (int i = 0; i < playerItems.length; i++) {
                        playerItems[i].isSelected = false;
                    }
                }
            });
        }
    }

    @SuppressLint("SimpleDateFormat")
    protected void onPlayerClick(Dialog dialog, @NonNull PlayerListItem playerListItem) {
        this.selectedEnemy = Multiplayer.getEnemy(playerListItem.name);
        if (this.selectedEnemy == null) {
            initPlayerList(dialog);
            initMessageList(dialog);
            return;
        }
        ArrayList<String> list = new ArrayList<>();
        if (Multiplayer.instance != null) {
            for (Multiplayer.Message message : Multiplayer.getPlayerMessageList(playerListItem.name)) {
                Date date = new Date(message.createdAt);
                SimpleDateFormat dateFormat = new SimpleDateFormat("hh:mm:ss");
                list.add(dateFormat.format(date) + " > " + message.message);
            }
            if (list.size() == 0) {
                list.add(this.context.getString(R.string.no_message_from_player, playerListItem.name));
            }
        }
        ArrayAdapter<String> dataAdapter = new ArrayAdapter<>(this.context, R.layout.custom_list_content, R.id.list_content, list);
        ListView messageList = dialog.findViewById(R.id.message_list);
        messageList.setAdapter(dataAdapter);
    }

    private void initMessageList(@NonNull Dialog dialog) {
        ListView messageList = dialog.findViewById(R.id.message_list);
        TextView emptyTextView = dialog.findViewById(16908292);
        if (emptyTextView != null) {
            emptyTextView.setText(R.string.please_choose_player);
            messageList.setEmptyView(emptyTextView);
        }
    }

    public static class PlayerAdapter extends ArrayAdapter<PlayerListItem> {
        private final Context context;
        private final int layoutResourceId;
        private PlayerListItem[] data;

        public PlayerAdapter(Context context, int layoutResourceId, PlayerListItem[] data) {
            super(context, layoutResourceId, data);
            this.data = null;
            this.layoutResourceId = layoutResourceId;
            this.context = context;
            this.data = data;
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            PlayerListItemHolder holder;
            View row = convertView;
            if (row == null) {
                LayoutInflater inflater = ((Activity) this.context).getLayoutInflater();
                row = inflater.inflate(this.layoutResourceId, parent, false);
                holder = new PlayerListItemHolder();
                holder.container = row.findViewById(R.id.player_list_item);
                holder.icon = row.findViewById(R.id.player_icon);
                holder.name = row.findViewById(R.id.player_name);
                row.setTag(holder);
            } else {
                holder = (PlayerListItemHolder) row.getTag();
            }
            PlayerListItem playerListItem = this.data[position];
            holder.name.setText(playerListItem.name);
            holder.icon.setImageResource(playerListItem.icon);
            if (playerListItem.isSelected) {
                holder.container.setBackgroundColor(-14540254);
            } else {
                holder.container.setBackgroundColor(-16777216);
            }
            return row;
        }

        private static class PlayerListItemHolder {
            LinearLayout container;
            ImageView icon;
            TextView name;

            private PlayerListItemHolder() {
            }
        }
    }

    public static class PlayerListItem {
        public int icon;
        public boolean isSelected = false;
        public String name;

        public PlayerListItem(int icon, String name) {
            this.icon = icon;
            this.name = name;
        }
    }
}
