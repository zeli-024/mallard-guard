# Item eligibility

Item Assignments controls which held items can guard and their optional block limits. Shields have a separate recognition screen. Punchy presets use the same assignment rules.

Search by name, ID, tooltip text, `#tag` or `@mod`. Category filters narrow the results; they do not change assignments. The Assigned panel shows every rule, even when it does not match the search.

Left-click allows an item; right-click blocks it. Clicking an assigned item removes it. Select Results includes offscreen matches; Clear Results removes their assignments. Escape cancels a pending bulk edit. Apply returns edits to the parent screen; Apply in settings saves them.

## Rules

Enter rules in Assign Rule, separately from the search field.

| Rule | Matches |
| --- | --- |
| `minecraft:stone` | One item ID |
| `#minecraft:planks` | An item tag |
| `@tacz` | All items in that mod namespace |
| `tacz` | Item IDs or tag IDs containing that text |
| `!tacz:rifle` | An exception to broader rules in the same list |

Removing an item selected by a group rule adds an `!item_id` exception. If both lists select it, both get an exception. Assigning it again removes the exception from the chosen list. Exceptions also appear in Assigned, where they can be removed. They affect only their own list; a matching blacklist rule still wins over the whitelist.

Display names are searchable but do not determine eligibility. Mod rules also match items added to that namespace later.

## Guarding requirements

Empty Hand must be enabled and both hands must be empty. An occupied, ineligible hand does not let the other hand guard empty-handed.

Tool Requirement offers three modes:

- **Tools Without Use:** attack-damage tools without a use action.
- **All Tools:** attack-damage tools, including those with use actions.
- **Any Held Item:** no tool requirement.

Recognized shields and explicit whitelist rules qualify separately. The blacklist applies in every mode. None of these modes enables empty-hand guarding.

## Preset ownership

Use Only This Preset reserves matching items for that preset, even when disabled. Disabling it offers a choice to keep or clear ownership. Allow Other Presets releases the reservation; deleting a custom preset removes it. Replacing conflicting ownership requires confirmation.

Batch assignments use one shared pool for the selected presets. Blacklists still apply. Renaming a preset does not change its ID.
