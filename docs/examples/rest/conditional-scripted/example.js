switch (context.request.path) {
    case "/users":
        respond().withFile('users.json');
        break;

    case "/pets":
        respond().withFile('pets.json');
        break;
}
