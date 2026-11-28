# Changelog

Each pull request adds its own bullets at the top of **Unreleased**, written for players. When a merge lands, the Discord message shows only the bullets that pull request added.

Push a version tag to publish. Alpha and beta releases use only **Unreleased**. Stable releases include those new notes under the tagged version, followed by every alpha/beta section since the previous stable release, newest first. The first stable release includes all earlier alpha/beta sections. The same notes go to CurseForge, Modrinth, and GitHub.

After publishing, the action commits the next development version and changelog update directly to **main**. It renames **Unreleased** to the tagged version and starts a new empty **Unreleased** above it; earlier sections stay in place without duplication. If the unpublished notes on main changed during the release, they are left untouched for manual reconciliation. A stable release with no new changes can publish the accumulated alpha/beta notes; its empty version heading still marks the cutoff for the next stable release. Concurrent pushes are never overwritten; rerun a rejected update after main advances. The action needs permission to push to main.

## Unreleased

## 0.0.2

- The pottery wheel fills the block it stands on and the lower half of the block above. The outline is that whole shape, so nothing else can be placed there. Breaking either part drops one wheel, and a wheel from an older world grows the upper half when the space above it is empty.
- Right-click the table with a blueprint to set it on the wheel. That uses one blueprint, even in Creative. Right-click the table with an empty hand to take it back. Setting another blueprint returns the one that was there. Breaking the wheel in Survival drops the blueprint along with any clay still on it.
- The blueprint's shape stands on all four edges of the table.
- Editing a stack of raw templates saves the shape onto one blueprint and leaves the rest of the stack raw.
- The template editor says left-click shapes the outside and right-click creates the inside.
- Pottery on a shelf sits on the plank, a little larger than before, and shows the liquid inside.
- Placed pottery keeps an even shade from one layer of the shape to the next.
- A wide mouth is a Bowl, a narrow mouth is a Jug, and any other shape is Pottery. A raw piece is named Raw Bowl, Raw Jug, or Raw Pottery.
- The creative tab contains the pottery wheel and the raw pottery template.
- On NeoForge 1.20.6, pottery items render in the inventory.
- A raw or fired piece in your hand or inventory has the shape that was thrown. Raw clay looks like clay, painted clay and fired pottery use a bright dye, and a closed pot keeps its lid.
- Raw pottery's tooltip says it needs firing. Undyed clay also says it can be dyed, and a vessel says how many millilitres it will hold once it is fired.
- A clay ball on the wheel is used up. The wheel stops when the shape is finished. Take the piece off then and you get raw pottery of that shape. Take it off earlier and you get the shape it had reached.
- Undyed raw pottery can go back on the wheel and be reshaped toward another blueprint.
- Craft raw pottery with a dye to paint it. Smelt raw pottery, painted or plain, and it fires into a usable vessel with the same shape and colour.

## 0.0.1

- Picking up clay unlocks recipes for the pottery wheel and raw template. Mining either half of the wheel returns the wheel in Survival.
- Nearby players see the wheel spin together, and a wheel keeps its shaping progress when its chunk unloads.
- The liquid surface in a placed vessel rises as you pour. Placed lids can be changed only within reach.
- A jug, jar, or pot holds liquid, and you can see the liquid when you look inside. Use it on water or lava, flowing or still, or on another fluid, and it fills without using up the source. A waterlogged block or a bubble column does not fill it.
- The tooltip shows how many millilitres it holds, and the liquid and how full it is when there is some. Water's name is blue.
- Use a vessel in the air, or right-click a placed one with an empty hand, to look inside and put a lid on or take it off. The lid sits just above the rim, a little wider than the mouth, with a small handle.
- Shift-right-click a placed piece with an empty hand to pick that piece up. Breaking the block breaks only the piece you are looking at.
- Pour from one filled vessel into a placed vessel. A bucket does not pour or scoop. With the lid on, you cannot fill the vessel or pour.
- A placed vessel with the lid off lights the block when its liquid's source block does. Lava does. The lid puts the light out.
- NeoForge runs on Minecraft 1.20.6, 1.21.1, and 26.3. Fabric runs on 26.3.
- A pottery wheel spins when you use it with an empty hand. Use a clay ball on it and the clay forms toward the blueprint installed on the wheel. Take the piece off when you want it. The clay ball stays in your hand.
- Edit a raw pottery template. The left mouse button sets the outer radius and the right sets the inner. Saving turns it into a pottery blueprint. A blueprint can be edited again, set on the wheel, or crafted by itself back into a blank raw template.
- A sealed shape is a pot, a jar, or a jug from how wide the mouth is next to the widest point. Wider than nine tenths is a pot, under half is a jug, and between those is a jar. A blueprint's tooltip names the type and how many millilitres it holds. A shape that is not a vessel says Lump.
- Finished pottery can sit in four places in one block. Pick a piece back up.
