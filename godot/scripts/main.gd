extends Node3D

const WALK_SPEED := 5.0
const LOOK_SENSITIVITY := 0.0025

var player: CharacterBody3D
var camera_pivot: Node3D
var camera: Camera3D
var status_label: Label
var house_built := false
var wood := 0
var stone := 0
var message_time := 0.0

func _ready() -> void:
	_build_world()
	_build_player()
	_build_hud()
	_show_message("Bienvenida a Cernego. Explora y recoge madera y piedra.")

func _build_world() -> void:
	var sun := DirectionalLight3D.new()
	sun.rotation_degrees = Vector3(-48, -32, 0)
	sun.light_energy = 1.2
	add_child(sun)

	var ground := MeshInstance3D.new()
	var ground_mesh := PlaneMesh.new()
	ground_mesh.size = Vector2(100, 100)
	ground.mesh = ground_mesh
	var grass := StandardMaterial3D.new()
	grass.albedo_color = Color(0.24, 0.39, 0.19)
	ground.material_override = grass
	add_child(ground)

	_add_box("Camino", Vector3(0, 0.025, 3), Vector3(5, 0.05, 34), Color(0.48, 0.39, 0.27))
	_add_box("Solar de la casa", Vector3(0, 0.04, -5), Vector3(6.5, 0.08, 5.5), Color(0.48, 0.42, 0.32))
	for i in range(12):
		var x := -13.0 + float(i % 6) * 5.2
		var z := -12.0 - float(i / 6) * 9.0
		_add_tree(Vector3(x, 0, z))
	_add_tree(Vector3(13, 0, 5))
	_add_tree(Vector3(-14, 0, 8))
	_add_resource(Vector3(5, 0.45, 1), Color(0.43, 0.45, 0.48), "Piedra")
	_add_resource(Vector3(-6, 0.45, -3), Color(0.43, 0.45, 0.48), "Piedra")

func _build_player() -> void:
	player = CharacterBody3D.new()
	player.position = Vector3(0, 1, 8)
	add_child(player)
	var body := CollisionShape3D.new()
	var capsule := CapsuleShape3D.new()
	capsule.radius = 0.38
	capsule.height = 1.8
	body.shape = capsule
	player.add_child(body)
	var model := MeshInstance3D.new()
	var mesh := CapsuleMesh.new()
	mesh.radius = 0.38
	mesh.height = 1.8
	model.mesh = mesh
	var mat := StandardMaterial3D.new()
	mat.albedo_color = Color(0.72, 0.49, 0.31)
	model.material_override = mat
	player.add_child(model)
	camera_pivot = Node3D.new()
	camera_pivot.position.y = 1.4
	player.add_child(camera_pivot)
	camera = Camera3D.new()
	camera.position = Vector3(0, 2.4, 6.5)
	camera.rotation_degrees.x = 14
	camera.current = true
	camera_pivot.add_child(camera)

func _build_hud() -> void:
	var layer := CanvasLayer.new()
	add_child(layer)
	var panel := PanelContainer.new()
	panel.position = Vector2(18, 18)
	layer.add_child(panel)
	var column := VBoxContainer.new()
	panel.add_child(column)
	var title := Label.new()
	title.text = "VALLE DE CERNEGO"
	title.add_theme_font_size_override("font_size", 22)
	column.add_child(title)
	status_label = Label.new()
	status_label.text = "Madera: 0   Piedra: 0"
	column.add_child(status_label)
	var help := Label.new()
	help.text = "WASD: mover · E: recoger/construir · Ratón: mirar"
	column.add_child(help)

func _physics_process(delta: float) -> void:
	if player == null:
		return
	var input := Input.get_vector("move_left", "move_right", "move_forward", "move_back")
	var direction := Vector3(input.x, 0, input.y).rotated(Vector3.UP, camera_pivot.rotation.y)
	player.velocity.x = direction.x * WALK_SPEED
	player.velocity.z = direction.z * WALK_SPEED
	if not player.is_on_floor():
		player.velocity.y -= 20.0 * delta
	else:
		player.velocity.y = -0.1
	player.move_and_slide()
	if message_time > 0:
		message_time -= delta
	elif status_label:
		_update_status()

func _unhandled_input(event: InputEvent) -> void:
	if event is InputEventMouseMotion and Input.is_mouse_button_pressed(MOUSE_BUTTON_RIGHT):
		camera_pivot.rotate_y(-event.relative.x * LOOK_SENSITIVITY)
		camera.rotation.x = clamp(camera.rotation.x - event.relative.y * LOOK_SENSITIVITY, -0.8, 0.25)
	if event is InputEventKey and event.pressed and not event.echo and event.keycode == KEY_E:
		_interact()

func _interact() -> void:
	var nearest := ""
	var nearest_distance := 3.0
	for item in get_tree().get_nodes_in_group("resources"):
		var d: float = player.global_position.distance_to(item.global_position)
		if d < nearest_distance:
			nearest_distance = d
			nearest = item.get_meta("kind", "")
	if nearest == "Madera":
		wood += 2
		_show_message("Has recogido 2 de madera.")
	elif nearest == "Piedra":
		stone += 2
		_show_message("Has recogido 2 de piedra.")
	elif player.global_position.distance_to(Vector3(0, 0, -5)) < 5.0:
		if not house_built and wood >= 10 and stone >= 4:
			wood -= 10
			stone -= 4
			house_built = true
			_add_house()
			_show_message("¡Tu casa de Cernego está construida!")
		elif house_built:
			_show_message("Esta es tu casa. ¡Pronto podrás decorarla!")
		else:
			_show_message("Para construir: necesitas 10 madera y 4 piedra.")
	else:
		_show_message("Acércate a un árbol, una piedra o el solar de tu casa.")
	_update_status()

func _add_house() -> void:
	var wall := Color(0.72, 0.62, 0.45)
	var roof := Color(0.35, 0.18, 0.14)
	_add_box("Casa", Vector3(0, 1.25, -5), Vector3(5, 2.5, 4), wall)
	_add_box("Tejado", Vector3(0, 2.8, -5), Vector3(5.8, 0.35, 4.8), roof)
	_add_box("Puerta", Vector3(0, 0.85, -2.96), Vector3(0.9, 1.7, 0.08), Color(0.25, 0.13, 0.07))

func _add_tree(pos: Vector3) -> void:
	var trunk := _add_box("Tronco", pos + Vector3(0, 1, 0), Vector3(0.45, 2, 0.45), Color(0.27, 0.16, 0.08))
	var crown := MeshInstance3D.new()
	var sphere := SphereMesh.new()
	sphere.radius = 1.35
	sphere.height = 2.5
	crown.mesh = sphere
	crown.position = pos + Vector3(0, 2.7, 0)
	var green := StandardMaterial3D.new()
	green.albedo_color = Color(0.12, 0.29, 0.12)
	crown.material_override = green
	add_child(crown)
	trunk.add_to_group("resources")
	trunk.set_meta("kind", "Madera")

func _add_resource(pos: Vector3, color: Color, kind: String) -> void:
	var node := _add_box(kind, pos, Vector3(0.9, 0.8, 0.9), color)
	node.add_to_group("resources")
	node.set_meta("kind", kind)

func _add_box(label: String, pos: Vector3, size: Vector3, color: Color) -> MeshInstance3D:
	var node := MeshInstance3D.new()
	node.name = label
	var mesh := BoxMesh.new()
	mesh.size = size
	node.mesh = mesh
	var mat := StandardMaterial3D.new()
	mat.albedo_color = color
	node.material_override = mat
	node.position = pos
	add_child(node)
	return node

func _show_message(value: String) -> void:
	if status_label:
		status_label.text = value + "\nMadera: %d   Piedra: %d" % [wood, stone]
		message_time = 4.0

func _update_status() -> void:
	status_label.text = "Madera: %d   Piedra: %d" % [wood, stone]
