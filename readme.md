# MMOItem Updater

這是一個 Bukkit/Spigot/Paper 插件，主要功能是讓伺服器管理員可以強制更新玩家身上與背包中，所有 MMOItems 的物品版本。本插件可以確保玩家身上的物品隨時與伺服器上最新的 MMOItems 設定同步。

## 功能

  * **強制更新物品**: 強制更新玩家身上及背包內所有 MMOItems 至最新版本。
  * **權限管理**: 透過權限節點，管理員可以精準控制誰可以使用更新指令。
  * **指定玩家更新**: 管理員可以指定特定玩家進行更新，也可以更新自己的物品。

## 指令與權限

| 指令 | 說明 | 權限 |
| --- | --- | --- |
| `/updateitems` | 更新自己的 MMOItems | `mmoupdater.update` |
| `/updateitems [玩家名稱]` | 更新指定玩家的 MMOItems | `mmoupdater.update.others` |

**別名**: `/updateitem`, `/revisionupdate`

## 安裝與設定

1.  **下載**: 從發布頁面下載最新的 JAR 檔案。
2.  **相依性**: 確認您的伺服器已經安裝 MMOItems 與 MythicLib。
3.  **安裝**: 將下載的 JAR 檔案放入您伺服器的 `plugins` 資料夾。
4.  **重新啟動**: 重新啟動您的伺服器，插件即可生效。

## 設定檔

本插件的設定檔 (`plugin.yml`) 包含以下內容：

```yaml
name: mmoitemUpdater
version: '1.0'
main: me.ninepin.mmoitemUpdater.MmoitemUpdater
api-version: '1.20'
prefix: mmoitemUpdater
authors: [ ninepin ]
description: mmoitemUpdater
commands:
  updateitems:
    description: Force update MMOItems to latest revision
    usage: /<command> [player]
    permission: mmoupdater.update
    aliases: [ updateitem, revisionupdate ]

permissions:
  mmoupdater.update:
    description: Allow updating items
    default: true
  mmoupdater.update.others:
    description: Allow updating other players' items
    default: op
```
