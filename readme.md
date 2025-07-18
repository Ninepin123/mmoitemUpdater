# MMOItems 強制更新器 (mmoitemUpdater)

這是一個為 Minecraft 伺服器設計的輔助插件，主要功能是自動並強制更新玩家背包中所有 `MMOItems` 物品到最新的版本 (Revision ID)，確保玩家持有的物品與伺服器上的設定同步。

## 功能特色

  * **全自動更新**：插件會定期在背景掃描所有在線玩家的背包，並自動更新過時的 MMOItems 物品。
  * **多樣的觸發機制**：除了定時更新，也可以設定在特定事件發生時觸發更新，例如：
      * 玩家加入遊戲
      * 玩家打開背包
      * 玩家使用物品
      * 玩家重生
      * 玩家切換世界
  * **高效能設計**：為了將對伺服器性能的影響降到最低，插件採用了多種優化技術：
      * **非同步處理**：大部分的檢查與更新都在背景異步執行，避免造成主線程卡頓。
      * **智慧檢查**：可選擇只檢查背包內容有發生變化的玩家，大幅減少不必要的掃描。
      * **玩家冷卻**：可以設定玩家被檢查後的冷卻時間，避免在短時間內重複掃描同一個玩家。
      * **流量控制**：可以限制每個遊戲刻 (Tick) 檢查的最大玩家數量。
  * **高度可自訂性**：
      * **世界白名單**：可指定插件只在特定的世界中運作。
      * **物品黑名單**：可設定某些特定的 MMOItems 物品永遠不會被自動更新。
      * **自訂訊息**：可以完全自訂物品更新後發送給玩家的提示訊息。
  * **手動指令**：提供管理員指令，可以立即為特定玩家或自己強制更新物品。
  * **統計資訊**：提供指令來查看插件運作的統計數據，如總檢查次數、總更新物品數等。

## 指令與權限

| 指令 | 描述 | 權限 | 預設 |
| :--- | :--- | :--- | :--- |
| `/updateitems` | 為自己強制更新所有MMOItems。 | `mmoupdater.update` | 全部玩家 |
| `/updateitems [玩家名稱]` | 強制更新指定玩家的所有MMOItems。 | `mmoupdater.update.others`| OP |

**指令別名**: `updateitem`, `revisionupdate`

## 安裝需求

  * **MMOItems**: 本插件為 MMOItems 的附屬插件，必須先安裝 MMOItems 才能正常運作。

## 設定檔案 (`config.yml`)

您可以透過 `config.yml` 檔案來詳細設定插件的各項功能。

```yaml
# ============================================
# MMOItems 強制更新器配置檔案
# 版本: 1.0.0
# ============================================

# 強制物品更新設定
forced-item-update:
  # 是否啟用完整背包檢查
  enabled: true

  # 定時檢查間隔（秒）
  schedule-interval: 1

  # 性能優化設定
  performance:
    # 每次檢查的最大玩家數量（避免同時檢查太多玩家）
    max-players-per-tick: 3

    # 是否啟用智能檢查（只檢查背包有變化的玩家）
    smart-check: false

    # 檢查冷卻時間（玩家上次更新後多久才再次檢查，秒）
    player-cooldown: 5

    # 是否啟用異步檢查
    async-check: true

  # 在哪些事件時觸發完整背包檢查
  trigger-events:
    join: true              # 加入遊戲時
    inventory-open: true    # 打開背包時
    item-use: true         # 使用物品時
    respawn: true          # 重生時
    world-change: true     # 切換世界時

  # 更新時的訊息設定
  notifications:
    enabled: true
    message: "&a系統自動更新了你的 {count} 個物品到最新版本！"
    # 是否只顯示大量更新的訊息（避免1個物品也顯示）
    minimum-count-to-show: 3

  # 白名單世界（僅在這些世界檢查），留空則代表所有世界
  whitelist-worlds: []
  # 範例:
  # - "world"
  # - "world_nether"

  # 黑名單物品ID（永不更新）
  blacklist-items: []
  # 範例:
  # - "QUEST_ITEM_1"
  # - "LEGACY_SWORD"
```

## 運作原理

插件的核心原理是比對玩家背包中 MMOItems 的 **Revision ID** (版本ID) 與伺服器端物品模板中的 **Revision ID**。當插件發現玩家持有的物品版本ID低於伺服器上最新的版本ID時，便會觸發更新機制，將該物品的資料重鑄 (Reforge) 為最新版本，同時保留物品原有的等級、寶石等屬性。整個過程可以透過定時任務或玩家的特定行為來觸發。
