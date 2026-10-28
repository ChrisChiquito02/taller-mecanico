@Service
public class tallerService {

    private final tallerRepository tallerRepository;

    public tallerService(tallerRepository tallerRepository) {
        this.tallerRepository = tallerRepository;
    }

    public tallerResponseDTO registrarTaller(tallerRequestDTO request) {
        Optional<tallerEntity> existente = tallerRepository.findByRfc(request.getRfc());
        if (existente.isPresent()) {
            throw new DuplicateTallerException("el taller con este rfc ya existe");
        }

        tallerEntity nuevoTaller = tallerEntity.builder()
                .id(GeneradorUuidV7.generar())
                .nombre(request.getNombre())
                .rfc(request.getRfc())
                .email(request.getEmail())
                .estatus("activo")
                .build();

        tallerEntity guardado = tallerRepository.save(nuevoTaller);
        return tallerResponseDTO.fromEntity(guardado);
    }
}