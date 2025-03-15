describe('Backend Ready Reader', () => {
  const line = "[11:07:40] [main/INFO] [dev.ftb.app.AppMain]: Backend Ready! Port=13377 OneTimeToken=90c87c45-a86c-4cbf-99d3-1735b5a772e6"
  
  test('should parse port', () => {
    const port = parseInt(line.match(/Port=(\d+)/)![1]);
    const secret = line.match(/OneTimeToken=(\w+-\w+-\w+-\w+-\w+)/)![1];
    
    expect(port).toBe(13377);
    expect(secret).toBe("90c87c45-a86c-4cbf-99d3-1735b5a772e6");
  });
});
