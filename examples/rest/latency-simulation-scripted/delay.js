
switch (context.request.path) {
    case '/scripted-exact-delay':
        respond()
            .withStatusCode(200)
            .withContent('Belated hello')
            .withDelay(500);

        break;

    case '/scripted-range-delay':
        respond()
            .withStatusCode(200)
            .withContent('Belated hello')
            .withDelayRange(200, 400);

        break;
}
