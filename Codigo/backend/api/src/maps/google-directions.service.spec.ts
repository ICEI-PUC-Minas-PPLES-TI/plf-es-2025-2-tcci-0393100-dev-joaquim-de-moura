import axios from 'axios';
import { GoogleDirectionsService } from './google-directions.service';

jest.mock('axios');
const mockedAxios = axios as jest.Mocked<typeof axios>;

const VALID_RESPONSE = {
  data: {
    status: 'OK',
    routes: [
      {
        legs: [
          {
            distance: { value: 5000 },
            duration: { value: 600 },
          },
        ],
        overview_polyline: { points: 'encodedPolylineHere' },
      },
    ],
  },
};

describe('GoogleDirectionsService — Integração com API de Mapas', () => {
  let service: GoogleDirectionsService;

  beforeEach(() => {
    service = new GoogleDirectionsService();
    jest.clearAllMocks();
  });

  describe('getRoute — resposta normal', () => {
    it('deve retornar distância, duração e polyline corretamente', async () => {
      mockedAxios.get.mockResolvedValueOnce(VALID_RESPONSE);

      const result = await service.getRoute(-19.7, -44.8, -19.75, -44.85, 'FAKE_KEY');

      expect(result.distanceMeters).toBe(5000);
      expect(result.durationSeconds).toBe(600);
      expect(result.encodedPolyline).toBe('encodedPolylineHere');
    });

    it('deve montar a URL com as coordenadas e a chave', async () => {
      mockedAxios.get.mockResolvedValueOnce(VALID_RESPONSE);

      await service.getRoute(-19.7, -44.8, -19.75, -44.85, 'MY_API_KEY');

      const calledUrl = mockedAxios.get.mock.calls[0][0] as string;
      expect(calledUrl).toContain('-19.7,-44.8');
      expect(calledUrl).toContain('-19.75,-44.85');
      expect(calledUrl).toContain('MY_API_KEY');
    });
  });

  describe('getRoute — queda e erros da API', () => {
    it('deve lançar erro quando status não é OK (chave inválida)', async () => {
      mockedAxios.get.mockResolvedValueOnce({
        data: { status: 'REQUEST_DENIED', error_message: 'Invalid API key' },
      });

      await expect(
        service.getRoute(-19.7, -44.8, -19.75, -44.85, 'INVALID_KEY'),
      ).rejects.toThrow('REQUEST_DENIED');
    });

    it('deve lançar erro quando a API retorna ZERO_RESULTS (rota não encontrada)', async () => {
      mockedAxios.get.mockResolvedValueOnce({
        data: { status: 'ZERO_RESULTS', routes: [] },
      });

      await expect(
        service.getRoute(-19.7, -44.8, -19.75, -44.85, 'KEY'),
      ).rejects.toThrow('ZERO_RESULTS');
    });

    it('deve lançar erro quando resposta não tem legs (dados incompletos)', async () => {
      mockedAxios.get.mockResolvedValueOnce({
        data: {
          status: 'OK',
          routes: [
            {
              legs: [{ distance: null, duration: null }],
              overview_polyline: { points: '' },
            },
          ],
        },
      });

      await expect(
        service.getRoute(-19.7, -44.8, -19.75, -44.85, 'KEY'),
      ).rejects.toThrow();
    });

    it('deve lançar erro quando a API de Mapas está fora do ar (timeout/rede)', async () => {
      mockedAxios.get.mockRejectedValueOnce(new Error('Network Error'));

      await expect(
        service.getRoute(-19.7, -44.8, -19.75, -44.85, 'KEY'),
      ).rejects.toThrow('Network Error');
    });

    it('deve lançar erro quando a API retorna timeout', async () => {
      mockedAxios.get.mockRejectedValueOnce(new Error('timeout of 5000ms exceeded'));

      await expect(
        service.getRoute(-19.7, -44.8, -19.75, -44.85, 'KEY'),
      ).rejects.toThrow('timeout');
    });

    it('deve lançar erro quando não há rotas disponíveis', async () => {
      mockedAxios.get.mockResolvedValueOnce({
        data: { status: 'OK', routes: [] },
      });

      await expect(
        service.getRoute(-19.7, -44.8, -19.75, -44.85, 'KEY'),
      ).rejects.toThrow();
    });
  });
});
