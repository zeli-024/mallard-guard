# Item eligibility

Item Assignments combines held-item eligibility with optional per-item block counts. Shields retain their separate recognition assignment screen. Punchy preset, supporting-hand and weapon assignment browsers use the same allow/block rule model.

Search supports names, IDs, tooltip text, #tags and @mod namespaces. For example, Two-Handed can match a mod's tooltip. Category checkboxes filter Blocks, Tools, Consumables, Shields and Other; multiple categories include either match. Filters change browsing only. Vanilla tooltips, including mod-added lines, appear on item hover.

Left-click allows an unassigned item; right-click blocks it. Either click removes an assigned item, including inherited selection through a group rule. Assign Rule adds the separately entered rule; it does not turn the search query into a saved rule. Select Results applies to every filtered result, including offscreen items. Clear Results removes their assignments. Bulk edits and tooltip searches can display progress; Escape cancels a pending bulk edit. Apply returns the draft to its parent; the main Apply saves server configuration.

Assigned is a separate, permanently visible panel beside the browsing grid. It shows every assignment regardless of search filters. Individual items show their icon, name, and ID. Group rules are labeled Mod, Tag, or Keyword. Each entry has an x button to remove it. This view is shared by eligibility, shield recognition, Punchy main-hand/preset, supporting-hand, and weapon item assignment screens.

| Saved rule | Meaning |
| --- | --- |
| `minecraft:stone` | Exact item ID |
| `#minecraft:planks` | Membership in that item tag |
| `@tacz` | Exact mod namespace |
| `tacz` | Substring in item IDs or item tag IDs |
| `!tacz:rifle` | Exclude that item from broader rules in this list |

Display names are searchable for browsing, but never decide gameplay eligibility. Each item rule list and block-count list supports up to 2,097,152 characters. There is no separate item-count cap. Whole namespaces can be stored as @mod rules; these also cover future items in that namespace. Large config saves travel in bounded chunks and apply only after complete receipt. Either click deselects an assigned item. If a mod, tag or keyword rule still selects it, the editor adds `!item_id` to that same list. When both lists select it, both receive an exception. Reassigning with left-click or right-click removes the exception from the chosen list. Exceptions appear in Assigned and can be removed there; they exclude an item from their own list, rather than blacklisting it globally. The general blacklist always wins.

Empty Hand requires both hands to be completely empty and its own toggle to be enabled. An occupied but ineligible hand cannot make the other empty hand eligible.

Tool Requirement offers Tools Without Use, All Tools and Any Held Item. The first excludes tools with a use action, All Tools accepts attack-damage tools with use actions too, and Any Held Item removes the tool requirement. Recognized shields and explicit whitelist rules qualify separately; all modes respect the blacklist. None enables empty-hand guarding by itself.

## Preset ownership

After editing preset assignments, choose Use Only This Preset or Allow Other Presets. Exclusive assignment reserves matching items for that preset, even while it is disabled; disabling offers a choice to clear or retain ownership. Deleting a custom preset removes its ownership. Conflicting exclusive ownership prompts before replacement. Batch editing several presets explicitly uses a shared pool. Blacklists still exclude an item from its owner's matching rules.

Preset IDs remain stable across renaming. Assignment matching is separate from editor layout, so future per-item definitions can use the same model.
