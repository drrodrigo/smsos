import Foundation
import AVFoundation
import UIKit
import Photos

/// O que a captura juntou ao acionar o SOS no iPhone.
struct CapturaResultado {
    var fotoURL: URL?
    var audioURL: URL?
    var audioSegundos: Int = 0
    var erros: [String] = []
    var temFoto: Bool { fotoURL != nil }
    var temAudio: Bool { audioURL != nil }
}

/// Ao acionar o SOS, tira uma FOTO (câmera frontal, sem abrir a tela) e grava um ÁUDIO curto do
/// ambiente — prova pra mandar aos guardiões. A localização já vem pelo Localizacao.swift.
///
/// Experimental e OPT-IN. Precisa das descrições no Info.plist (project.yml):
///   NSCameraUsageDescription, NSMicrophoneUsageDescription, NSPhotoLibraryAddUsageDescription.
///
/// ⚠️ ESCRITO MAS NÃO COMPILADO NEM TESTADO (não há toolchain Swift no ambiente de dev). Precisa de
/// um build de TestFlight pra validar. Ainda NÃO está ligado na tela — é o próximo passo.
final class Captura: NSObject {
    static let shared = Captura()

    private let session = AVCaptureSession()
    private let photoOutput = AVCapturePhotoOutput()
    private var recorder: AVAudioRecorder?
    private var onFoto: ((URL?, String?) -> Void)?

    // MARK: - Permissões

    static func pedirPermissoes(_ done: @escaping (Bool, Bool) -> Void) {
        AVCaptureDevice.requestAccess(for: .video) { camOk in
            AVAudioSession.sharedInstance().requestRecordPermission { micOk in
                DispatchQueue.main.async { done(camOk, micOk) }
            }
        }
    }

    static var temCamera: Bool { AVCaptureDevice.authorizationStatus(for: .video) == .authorized }
    static var temMic: Bool { AVAudioSession.sharedInstance().recordPermission == .granted }

    // MARK: - Captura (foto + áudio em paralelo)

    func capturar(audioSegundos: Int, completion: @escaping (CapturaResultado) -> Void) {
        var resultado = CapturaResultado()
        let grupo = DispatchGroup()

        grupo.enter()
        gravarAudio(segundos: audioSegundos) { url, erro in
            resultado.audioURL = url
            if url != nil { resultado.audioSegundos = audioSegundos }
            if let e = erro { resultado.erros.append(e) }
            grupo.leave()
        }

        grupo.enter()
        tirarFoto { url, erro in
            resultado.fotoURL = url
            if let e = erro { resultado.erros.append(e) }
            grupo.leave()
        }

        grupo.notify(queue: .main) { completion(resultado) }
    }

    // MARK: - Áudio

    private func gravarAudio(segundos: Int, done: @escaping (URL?, String?) -> Void) {
        guard Captura.temMic else { done(nil, "Sem permissão de microfone"); return }
        let sessao = AVAudioSession.sharedInstance()
        do {
            try sessao.setCategory(.playAndRecord, mode: .default)
            try sessao.setActive(true)
        } catch {
            done(nil, "Áudio: \(error.localizedDescription)"); return
        }
        let url = FileManager.default.temporaryDirectory
            .appendingPathComponent("sos_\(Int(Date().timeIntervalSince1970)).m4a")
        let settings: [String: Any] = [
            AVFormatIDKey: Int(kAudioFormatMPEG4AAC),
            AVSampleRateKey: 44_100,
            AVNumberOfChannelsKey: 1,
        ]
        do {
            let rec = try AVAudioRecorder(url: url, settings: settings)
            recorder = rec
            rec.record(forDuration: TimeInterval(segundos))
            DispatchQueue.main.asyncAfter(deadline: .now() + Double(segundos) + 0.3) {
                rec.stop()
                if FileManager.default.fileExists(atPath: url.path) {
                    done(url, nil)
                } else {
                    done(nil, "Áudio não gravou")
                }
            }
        } catch {
            done(nil, "Áudio: \(error.localizedDescription)")
        }
    }

    // MARK: - Foto (câmera frontal, sem preview)

    private func tirarFoto(done: @escaping (URL?, String?) -> Void) {
        guard Captura.temCamera else { done(nil, "Sem permissão de câmera"); return }
        let device = AVCaptureDevice.default(.builtInWideAngleCamera, for: .video, position: .front)
            ?? AVCaptureDevice.default(for: .video)
        guard let camera = device else { done(nil, "Sem câmera"); return }
        do {
            let input = try AVCaptureDeviceInput(device: camera)
            session.beginConfiguration()
            session.sessionPreset = .photo
            if session.canAddInput(input) { session.addInput(input) }
            if session.canAddOutput(photoOutput) { session.addOutput(photoOutput) }
            session.commitConfiguration()
            onFoto = done
            DispatchQueue.global(qos: .userInitiated).async { [weak self] in
                guard let self else { return }
                self.session.startRunning()
                // pequeno tempo pra câmera ajustar exposição antes do clique
                DispatchQueue.main.asyncAfter(deadline: .now() + 0.4) {
                    self.photoOutput.capturePhoto(with: AVCapturePhotoSettings(), delegate: self)
                }
            }
        } catch {
            done(nil, "Câmera: \(error.localizedDescription)")
        }
    }
}

extension Captura: AVCapturePhotoCaptureDelegate {
    func photoOutput(
        _ output: AVCapturePhotoOutput,
        didFinishProcessingPhoto photo: AVCapturePhoto,
        error: Error?
    ) {
        defer {
            session.stopRunning()
            session.inputs.forEach { session.removeInput($0) }
        }
        if let error = error {
            onFoto?(nil, "Foto: \(error.localizedDescription)"); return
        }
        guard let data = photo.fileDataRepresentation() else {
            onFoto?(nil, "Foto vazia"); return
        }
        let url = FileManager.default.temporaryDirectory
            .appendingPathComponent("sos_\(Int(Date().timeIntervalSince1970)).jpg")
        do {
            try data.write(to: url)
        } catch {
            onFoto?(nil, "Foto: \(error.localizedDescription)"); return
        }
        // Salva no Rolo da Câmera pra pessoa ver a prova (como a galeria no Android).
        if let img = UIImage(data: data) {
            UIImageWriteToSavedPhotosAlbum(img, nil, nil, nil)
        }
        onFoto?(url, nil)
    }
}
